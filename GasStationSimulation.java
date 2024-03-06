import javaSimulation.*;
import javaSimulation.Process;

public class GasStationSimulation extends Process {
    int noOfPumps; // Combined number of pumps
    int payed;
    double simPeriod = 300;
    Head pumps = new Head();
    Head waitingCars = new Head(); // Queue for all cars
    Head waitingForPaymentCars = new Head(); // Queue for cars waiting for payment
    Head cashiers = new Head();
    Random random = new Random(10);
    double totalServiceTime;
    int noOfServedCars, maxQueueLength, noOfCarsLeft, currentCarsOnStation = 0;
    int noOfCashiers;
    int maxCapacity = 60; // Maximum capacity of cars at the station
    long startTime = System.currentTimeMillis();

    GasStationSimulation(int pumps, int noOfCashiers) {
        this.noOfPumps = pumps;
        this.noOfCashiers = noOfCashiers;
    }

    public void actions() {
        for (int i = 0; i < noOfPumps; i++)
            new FuelPump().into(pumps);
        for (int i = 0; i < noOfCashiers; i++)
            new Cashier().into(cashiers);

        activate(new CarGenerator());
        hold(simPeriod + 1000000);
        report();
    }

    void report() {
        System.out.println(noOfPumps + " fuel pumps and " + noOfCashiers + " cashiers simulation");
        System.out.println("Number of cars served = " + noOfServedCars);
        System.out.println("Number of cars that left = " + noOfCarsLeft);
        System.out.println("Number of cars that paid: "+payed);
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance();
        fmt.setMaximumFractionDigits(2);
        System.out.println("Average service time = " + fmt.format(totalServiceTime / noOfServedCars));
        System.out.println("Maximum queue length = " + maxQueueLength);
        System.out.println("\nExecution time: " + fmt.format((System.currentTimeMillis() - startTime) / 1000.0) + " secs.\n");
    }

        // Car class modifications to ensure it looks for an available pump and waits for its turn
        class Car extends Process {
            double fuelLevel;
            FuelPump assignedPump; // Track the assigned fuel pump
    
            public Car(){
                    double mean = 30;
                    double std = 10;
                    // Generování množství paliva s normálním rozdělením a omezení hodnot mezi 0 a 100
                    double fuelLevel = random.nextGaussian() *std + mean;
                    this.fuelLevel = Math.max(0, Math.min(fuelLevel, 100)); //omezení kvůli předpokladu množství paliva v procentech
            }
    
            public void actions() {
                double arrivalTime = time();
                if (waitingCars.cardinal() + currentCarsOnStation < maxCapacity) {
                    into(waitingCars);
                    currentCarsOnStation++;
                    if (maxQueueLength < waitingCars.cardinal()) maxQueueLength = waitingCars.cardinal();
                    
                    // Continuously search for an available pump
                    while (assignedPump == null) {
                        for (Link l = pumps.first(); l != null; l = l.suc()) {
                            FuelPump pump = (FuelPump) l;
                        // In the Car class, when assigning a pump
                        if (!pump.isInUse()) {
                            pump.setInUse(this); // Lock the pump for this car
                            assignedPump = pump;
                            activate(pump);
                            break;
                        }

                        }
                        if (assignedPump == null) hold(1); // Wait and try again if no pump was available
                    }
                    
                    passivate(); // Wait for fueling and payment to complete
                    
                    out(); // Remove car from the simulation (or any queues it's in)
                    currentCarsOnStation--;
                    noOfServedCars++;
                    totalServiceTime += time() - arrivalTime;
                } else {
                    noOfCarsLeft++;
                }
            }
        }
    
        class FuelPump extends Process {
            private Car inUseBy = null; // Tracks which car is using the pump, null if not in use
        
            public boolean isInUse() {
                return inUseBy != null; // The pump is in use if this is not null
            }
        
            public void setInUse(Car car) {
                this.inUseBy = car; // Assign the car to the pump
            }
        
                        // In the FuelPump class
            public void actions() {
                while (true) {
                    if (inUseBy != null) {
                        double fuelNeeded = 100 - inUseBy.fuelLevel;
                        double refuelingTime = calculateRefuelingTime(fuelNeeded);
                        hold(refuelingTime); // Simulate refueling

                        inUseBy.into(waitingForPaymentCars);
                        // Activate all cashiers; they will check if there are cars to process
                        for (Link l = cashiers.first(); l != null; l = l.suc()) {
                            Cashier cashier = (Cashier) l;
                            if (!cashier.isBusy) {
                                activate(cashier); // Activate only this cashier
                                break; // Exit after finding the first available cashier
                            }
                        }
                        passivate();
                    } else {
                        passivate();
                    }
                }
            }

        
            private double calculateRefuelingTime(double fuelNeeded) {
                return fuelNeeded / 10; // Simulates the refueling time based on the amount of fuel needed
            }
        }
        
    
        // Cashier class modifications to handle payment and unlock the fuel pump after payment
        class Cashier extends Process {
            boolean isBusy = false; // Flag to track if the cashier is currently busy
            public void actions() {
                while (true) {
                    if (!waitingForPaymentCars.empty() && !isBusy) {
                        this.isBusy = true;
                        Car car = (Car) waitingForPaymentCars.first();
                        car.out(); // Remove the car from the payment queue
                        hold(calculatePaymentTime()); // Simulate payment time
                        car.assignedPump.setInUse(null); // Unlock the fuel pump
                        activate(car); // Reactivate the car to proceed with leaving the station
                        payed++;
                        this.isBusy = false;
                    } else {
                        passivate(); // Wait for another car to arrive for payment
                    }
                }
            }
        private double calculatePaymentTime() {
            return random.negexp(1 / 10.0);
        
    }
}

    class CarGenerator extends Process {
        public void actions() {
            while (time() <= simPeriod) {
                activate(new Car());
                hold(random.negexp(1 / 2.0)); // Generate new cars at a random time
            }
        }
    }

    public static void main(String args[]) {
        activate(new GasStationSimulation(2,2));
        activate(new GasStationSimulation(3,2));
        activate(new GasStationSimulation(2,5));
    }
}
