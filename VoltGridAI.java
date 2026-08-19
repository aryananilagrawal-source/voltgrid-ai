import java.util.;

class Vehicle {

    int id;
    double soc;
    double batteryCapacity;
    double requiredEnergy;
    double chargingPower;
    String status;

    Vehicle(int id, double soc, double batteryCapacity) {

        this.id = id;
        this.soc = soc;
        this.batteryCapacity = batteryCapacity;

        this.requiredEnergy =
                batteryCapacity  (100 - soc)  100.0;

        this.chargingPower = 0;
        this.status = Waiting;
    }
}


class ChargingStation {

    String name;
    double capacity;
    ArrayListVehicle vehicles;

    ChargingStation(String name, double capacity) {

        this.name = name;
        this.capacity = capacity;
        this.vehicles = new ArrayList();
    }

    void addVehicle(Vehicle vehicle) {

        vehicles.add(vehicle);
    }

    void removeVehicle(Vehicle vehicle) {

        vehicles.remove(vehicle);
    }

    double getCurrentLoad() {

        double load = 0;

        for (Vehicle vehicle  vehicles) {
            load += vehicle.chargingPower;
        }

        return load;
    }
}


public class VoltGridAI {

    static Random random = new Random();

    static ArrayListChargingStation stations =
            new ArrayList();

    static int vehicleCounter = 0;

    static int completedVehicles = 0;

    static final double GRID_CAPACITY = 300;

    static final double GRID_THRESHOLD = 0.85;


     ------------------------------------------------
     CREATE CHARGING STATIONS
     ------------------------------------------------

    static void createStations() {

        stations.add(
                new ChargingStation(Residential, 70)
        );

        stations.add(
                new ChargingStation(Commercial, 100)
        );

        stations.add(
                new ChargingStation(Highway, 90)
        );

        stations.add(
                new ChargingStation(Fleet, 80)
        );
    }


     ------------------------------------------------
     AUTOMATICALLY CREATE NEW EV
     ------------------------------------------------

    static Vehicle createVehicle() {

        vehicleCounter++;

        double soc =
                15 + random.nextInt(76);

        double[] batteries =
                {40, 50, 60, 75, 80};

        double batteryCapacity =
                batteries[random.nextInt(batteries.length)];

        Vehicle vehicle =
                new Vehicle(
                        vehicleCounter,
                        soc,
                        batteryCapacity
                );

        return vehicle;
    }


     ------------------------------------------------
     AUTOMATIC EV ARRIVAL
     ------------------------------------------------

    static void automaticArrival() {

        double probability =
                random.nextDouble();

        if (probability  0.65) {

            Vehicle vehicle =
                    createVehicle();

            ChargingStation bestStation =
                    stations.get(0);

            for (ChargingStation station  stations) {

                if (station.vehicles.size()
                         bestStation.vehicles.size()) {

                    bestStation = station;
                }
            }

            bestStation.addVehicle(vehicle);

            System.out.println(
                    n[ARRIVAL] EV- +
                    vehicle.id +
                     arrived at  +
                    bestStation.name +
                     station
            );

            System.out.println(
                              SOC  +
                    String.format(%.1f, vehicle.soc) +
                    %
            );
        }
    }


     ------------------------------------------------
     COUNT TOTAL EVs
     ------------------------------------------------

    static int getTotalVehicles() {

        int total = 0;

        for (ChargingStation station  stations) {

            total += station.vehicles.size();
        }

        return total;
    }


     ------------------------------------------------
     GET TOTAL GRID LOAD
     ------------------------------------------------

    static double getTotalLoad() {

        double total = 0;

        for (ChargingStation station  stations) {

            total += station.getCurrentLoad();
        }

        return total;
    }


     ------------------------------------------------
     AI LOAD PREDICTION
     ------------------------------------------------

    static double predictFutureLoad(
            int hour,
            int numberOfVehicles,
            double currentLoad) {

        double predictedLoad;

        
          Simplified AI prediction model.
         
          In the final version this can be replaced
          by an actual TensorFlowKeras LSTM model.
         

        if (hour = 7 && hour = 10) {

            predictedLoad =
                    currentLoad  1.15
                    + numberOfVehicles  2;

        }

        else if (hour = 17 && hour = 21) {

            predictedLoad =
                    currentLoad  1.30
                    + numberOfVehicles  2.5;

        }

        else {

            predictedLoad =
                    currentLoad  0.85
                    + numberOfVehicles  1.5;
        }

        predictedLoad +=
                random.nextDouble()  10;

        return Math.max(0, predictedLoad);
    }


     ------------------------------------------------
     ALLOCATE CHARGING POWER
     ------------------------------------------------

    static void allocatePower() {

        ArrayListVehicle allVehicles =
                new ArrayList();

        for (ChargingStation station  stations) {

            allVehicles.addAll(
                    station.vehicles
            );
        }

        if (allVehicles.isEmpty()) {

            return;
        }

        double availablePower =
                GRID_CAPACITY 
                GRID_THRESHOLD;

        double powerPerVehicle =
                availablePower 
                allVehicles.size();


        for (Vehicle vehicle  allVehicles) {

            
              Maximum charging power
              of one EV is limited to 22 kW.
             

            vehicle.chargingPower =
                    Math.min(
                            22,
                            Math.max(
                                    3,
                                    powerPerVehicle
                            )
                    );

            vehicle.status =
                    Charging;
        }
    }


     ------------------------------------------------
     DYNAMIC LOAD BALANCING
     ------------------------------------------------

    static void dynamicLoadBalancing() {

        double totalLoad =
                getTotalLoad();

        double safeLimit =
                GRID_CAPACITY 
                GRID_THRESHOLD;


        if (totalLoad = safeLimit) {

            System.out.println(
                    [GRID] Load  +
                    String.format(
                            %.1f,
                            totalLoad
                    ) +
                     kW is within safe limit
            );

            return;
        }


        System.out.println(
                [WARNING] GRID OVERLOAD!
        );


        double excess =
                totalLoad - safeLimit;


        int vehicleCount =
                getTotalVehicles();


        double reduction =
                excess 
                Math.max(1, vehicleCount);


        
          Reduce the charging power
          of every active EV.
         

        for (ChargingStation station 
                stations) {

            for (Vehicle vehicle 
                    station.vehicles) {

                vehicle.chargingPower =
                        Math.max(
                                3,
                                vehicle.chargingPower
                                - reduction
                        );
            }
        }


        System.out.println(
                [REBALANCE]  +
                String.format(
                        %.1f,
                        excess
                ) +
                 kW excess load redistributed
        );
    }


     ------------------------------------------------
     CHARGE EVs
     ------------------------------------------------

    static void chargeVehicles() {

        for (ChargingStation station 
                stations) {

            for (Vehicle vehicle 
                    station.vehicles) {

                if (!vehicle.status.equals(
                        Charging)) {

                    continue;
                }


                
                  Each cycle represents
                  a small amount of time.
                 

                double energyAdded =
                        vehicle.chargingPower
                         0.1;


                vehicle.requiredEnergy -=
                        energyAdded;


                double socIncrease =
                        (energyAdded 
                        vehicle.batteryCapacity)
                         100;


                vehicle.soc +=
                        socIncrease;


                if (vehicle.soc = 95) {

                    vehicle.soc = 100;

                    vehicle.status =
                            Completed;
                }
            }
        }
    }


     ------------------------------------------------
     AUTOMATIC EV DEPARTURE
     ------------------------------------------------

    static void automaticDeparture() {

        for (ChargingStation station 
                stations) {

            IteratorVehicle iterator =
                    station.vehicles.iterator();


            while (iterator.hasNext()) {

                Vehicle vehicle =
                        iterator.next();


                if (vehicle.soc = 95) {

                    System.out.println(
                            [DEPARTURE] EV- +
                            vehicle.id +
                             completed charging 
                            +
                            and left  +
                            station.name
                    );


                    iterator.remove();

                    completedVehicles++;
                }
            }
        }
    }


     ------------------------------------------------
     DISPLAY SYSTEM STATUS
     ------------------------------------------------

    static void displayStatus(
            double predictedLoad) {

        System.out.println(
                n==============================================
        );

        System.out.println(
                          VOLTGRID AI STATUS
        );

        System.out.println(
                ==============================================
        );

        System.out.println(
                Predicted Load   +
                String.format(
                        %.1f,
                        predictedLoad
                ) +
                 kW
        );

        System.out.println(
                Actual Load      +
                String.format(
                        %.1f,
                        getTotalLoad()
                ) +
                 kW
        );

        System.out.println(
                Grid Capacity    +
                GRID_CAPACITY +
                 kW
        );

        System.out.println(
                Active EVs       +
                getTotalVehicles()
        );

        System.out.println(
                Completed EVs    +
                completedVehicles
        );


        System.out.println(
                ----------------------------------------------
        );


        for (ChargingStation station 
                stations) {

            System.out.println(
                    station.name +
                      EVs  +
                    station.vehicles.size() +
                      Load  +
                    String.format(
                            %.1f,
                            station.getCurrentLoad()
                    ) +
                     kW
            );


            for (Vehicle vehicle 
                    station.vehicles) {

                System.out.println(
                           EV- +
                        vehicle.id +
                          SOC  +
                        String.format(
                                %.1f,
                                vehicle.soc
                        ) +
                        %  Power  +
                        String.format(
                                %.1f,
                                vehicle.chargingPower
                        ) +
                         kW   +
                        vehicle.status
                );
            }
        }

        System.out.println(
                ==============================================
        );
    }


     ------------------------------------------------
     MAIN CONTROL LOOP
     ------------------------------------------------

    public static void main(String[] args)
            throws InterruptedException {

        createStations();


        
          Continuous VoltGrid AI control loop
         

        for (int cycle = 1;
             cycle = 100;
             cycle++) {


            int hour =
                    (cycle  10) % 24;


            System.out.println(
                    nnCONTROL CYCLE  +
                    cycle +
                      TIME  +
                    String.format(
                            %02d00,
                            hour
                    )
            );


             1. Automatically add EV
            automaticArrival();


             2. Get current system information
            int vehicles =
                    getTotalVehicles();

            double currentLoad =
                    getTotalLoad();


             3. AI predicts future demand
            double predictedLoad =
                    predictFutureLoad(
                            hour,
                            vehicles,
                            currentLoad
                    );


            System.out.println(
                    [AI] Predicted future load  +
                    String.format(
                            %.1f,
                            predictedLoad
                    ) +
                     kW
            );


             4. Allocate power
            allocatePower();


             5. Dynamic load balancing
            dynamicLoadBalancing();


             6. Charge EVs
            chargeVehicles();


             7. Automatically remove completed EVs
            automaticDeparture();


             8. Display live status
            displayStatus(
                    predictedLoad
            );


             9. Wait before next cycle
            Thread.sleep(500);
        }
    }
}
