const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '.env') });
const express = require('express');
const cors = require('cors');
const OpenAI = require('openai');

const app = express();
const PORT = process.env.PORT || 3001;
const P_GRID_MAX = 200.0;

app.use(cors());
app.use(express.json());

// Serve frontend assets
app.use(express.static(__dirname));
app.use(express.static(path.join(__dirname, '../')));

// Groq AI client setup
const ai = new OpenAI({
  apiKey: process.env.GROQ_API_KEY,
  baseURL: 'https://api.groq.com/openai/v1',
});

// Dynamic Load Balancing Endpoint
app.post('/api/load-balance', (req, res) => {
  try {
    let { bays } = req.body;
    if (!bays || !Array.isArray(bays)) {
      return res.status(400).json({ error: "Invalid bays array" });
    }

    const activeBays = bays.filter(b => b.status === "Charging");
    const totalRequested = activeBays.reduce((sum, b) => sum + (Number(b.reqPower) || 0), 0);

    if (totalRequested <= P_GRID_MAX) {
      activeBays.forEach(b => {
        b.allocatedPower = Number(b.reqPower);
      });
    } else {
      const highPrio = activeBays.filter(b => (b.priority || "").toUpperCase().includes("HIGH"));
      const standard = activeBays.filter(b => !(b.priority || "").toUpperCase().includes("HIGH"));

      let allocatedHigh = 0;
      highPrio.forEach(b => {
        b.allocatedPower = Number(b.reqPower);
        allocatedHigh += b.allocatedPower;
      });

      const remainingKw = Math.max(10.0, P_GRID_MAX - allocatedHigh);
      const standardWeights = standard.reduce((acc, b) => acc + (100 - (b.soc || 0)), 0) || 1;

      standard.forEach(b => {
        const share = ((100 - (b.soc || 0)) / standardWeights) * remainingKw;
        b.allocatedPower = Math.min(Number(b.reqPower), Math.max(10.0, Math.round(share * 10) / 10));
      });

      const totalAllocated = activeBays.reduce((sum, b) => sum + b.allocatedPower, 0);
      if (totalAllocated > P_GRID_MAX) {
        const scale = P_GRID_MAX / totalAllocated;
        activeBays.forEach(b => {
          b.allocatedPower = Math.floor(b.allocatedPower * scale * 10) / 10;
        });
      }
    }

    bays.forEach(b => {
      if (b.status !== "Charging") b.allocatedPower = 0;
    });

    const finalTotal = bays.reduce((sum, b) => sum + (b.allocatedPower || 0), 0);

    res.json({
      status: "success",
      totalLoad: Math.round(finalTotal * 10) / 10,
      transformerLimit: P_GRID_MAX,
      bays: bays
    });
  } catch (err) {
    console.error("Load balancing error:", err);
    res.status(500).json({ error: "Calculation failed" });
  }
});

// Telemetry Forecast Endpoint
app.get('/api/forecast', (req, res) => {
  res.json({
    intervals: ['T+15m', 'T+30m', 'T+45m', 'T+60m', 'T+75m', 'T+90m'],
    predictedKw: [135.0, 158.4, 182.0, 194.5, 172.0, 148.0],
    transformerLimit: 200.0
  });
});

// AI Copilot Endpoint
app.post('/api/chat', async (req, res) => {
  try {
    const { prompt, stationContext } = req.body;
    console.log("Processing query:", prompt);

    const completion = await ai.chat.completions.create({
      model: 'llama-3.1-8b-instant',
      messages: [
        {
          role: 'system',
          content: `You are VoltGrid Copilot, an AI energy management assistant. 
Station Limit: 200 kW. Current Load: ${stationContext?.totalLoad || 0} kW.
Explain charging prioritization and peak shaving decisions concisely in 2 sentences.`
        },
        { role: 'user', content: prompt }
      ],
      temperature: 0.3
    });

    const reply = completion.choices[0].message.content;
    console.log("Copilot response generated");
    res.json({ reply });
  } catch (err) {
    console.error("Groq AI Error:", err.message);
    res.status(500).json({ 
      reply: "Grid safely managed. High-priority fleet vehicles are given full allocation while standard bays are throttled to respect the 200 kW limit." 
    });
  }
});

app.listen(PORT, () => {
  console.log(`VoltGrid AI backend running on port ${PORT}`);
});