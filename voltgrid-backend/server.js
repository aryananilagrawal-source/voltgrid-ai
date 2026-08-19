// VoltGrid AI - Backend Server
// Provides 3 AI agent endpoints: load-balance, forecast, chat
// Uses Groq (free, fast) by default. Swap to Anthropic/OpenAI by changing the client config below.

const express = require('express');
const cors = require('cors');
const OpenAI = require('openai'); // Groq is OpenAI-SDK-compatible
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

// --- AI Client Setup ---
// Groq: free tier, no credit card, fast. Get a key at console.groq.com/keys
const ai = new OpenAI({
  apiKey: process.env.GROQ_API_KEY,
  baseURL: 'https://api.groq.com/openai/v1',
});
const FAST_MODEL = 'openai/gpt-oss-20b';
const CHAT_MODEL = 'openai/gpt-oss-20b';

// Helper: call the model and force JSON-only output
async function callAgent(systemPrompt, userPrompt, model = FAST_MODEL) {
  const completion = await ai.chat.completions.create({
    model,
    messages: [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userPrompt },
    ],
    temperature: 0.3,
  });
  const raw = completion.choices[0].message.content.trim();
  // Strip markdown code fences if the model adds them despite instructions
  const cleaned = raw.replace(/^```json\s*/i, '').replace(/```$/, '').trim();
  return cleaned;
}

// --- AGENT 1: Load Balancing Agent ---
// Input: { bays: [...], transformerMax: number }
// Output: { allocations: [{id, allocatedPower}], reasoning: string }
app.post('/api/load-balance', async (req, res) => {
  try {
    const { bays, transformerMax } = req.body;
    const activeBays = bays.filter((b) => b.status === 'Charging');

    const systemPrompt = `You are the Load Balancing Agent for an EV charging station. You allocate available power across charging bays without exceeding the transformer capacity.
Rules:
- Never let the sum of allocatedPower exceed transformerMax.
- High priority bays (fleet/emergency) get power first, up to their requested amount.
- Bays with SoC over 80% can be throttled more (charging slows near-full batteries anyway).
- Be fair: don't starve any active bay down to 0 unless absolutely necessary.
Respond with ONLY valid JSON, no markdown, no explanation outside the JSON:
{"allocations": [{"id": <bay id>, "allocatedPower": <number>}], "reasoning": "<one sentence explaining the key decision made this round>"}`;

    const userPrompt = `Transformer max capacity: ${transformerMax} kW.
Active bays: ${JSON.stringify(
      activeBays.map((b) => ({
        id: b.id,
        reqPower: b.reqPower,
        soc: b.soc,
        priority: b.priority,
      }))
    )}`;

    const result = await callAgent(systemPrompt, userPrompt);
    const parsed = JSON.parse(result);
    res.json(parsed);
  } catch (err) {
    console.error('load-balance error:', err.message);
    res.status(500).json({ error: 'Load balancing agent failed', detail: err.message });
  }
});

// --- AGENT 2: Forecasting Agent ---
// Input: { history: [numbers] }  (recent total load readings)
// Output: { predictedLoad: number, trend: string, reasoning: string }
app.post('/api/forecast', async (req, res) => {
  try {
    const { history } = req.body;

    const systemPrompt = `You are the Demand Forecasting Agent for an EV charging station network. Given recent total load readings (kW, most recent last), predict the load for the next period.
Respond with ONLY valid JSON, no markdown:
{"predictedLoad": <number>, "trend": "<rising|falling|stable>", "reasoning": "<one short sentence>"}`;

    const userPrompt = `Recent load history (kW, chronological): ${JSON.stringify(history)}`;

    const result = await callAgent(systemPrompt, userPrompt);
    const parsed = JSON.parse(result);
    res.json(parsed);
  } catch (err) {
    console.error('forecast error:', err.message);
    res.status(500).json({ error: 'Forecasting agent failed', detail: err.message });
  }
});

// --- AGENT 3: Driver Assistant Chat Agent ---
// Input: { message: string, context: { bays, forecast } }
// Output: { reply: string }
app.post('/api/chat', async (req, res) => {
  try {
    const { message, context } = req.body;

    const systemPrompt = `You are a helpful EV charging station assistant. You help drivers understand current bay availability, wait times, and get recommendations on where to plug in. Be concise and friendly - 2-3 sentences max. Use the live station data provided to give specific, grounded answers rather than generic advice.`;

    const userPrompt = `Current station data: ${JSON.stringify(context)}\n\nDriver question: ${message}`;

    const completion = await ai.chat.completions.create({
      model: CHAT_MODEL,
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userPrompt },
      ],
      temperature: 0.6,
    });

    res.json({ reply: completion.choices[0].message.content.trim() });
  } catch (err) {
    console.error('chat error:', err.message);
    res.status(500).json({ error: 'Chat agent failed', detail: err.message });
  }
});

app.get('/health', (req, res) => res.json({ status: 'ok' }));

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => console.log(`VoltGrid AI backend running on port ${PORT}`));
