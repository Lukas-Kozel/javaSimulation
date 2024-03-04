import javaSimulation.*;
import javaSimulation.Process;

public class GasStationSimulation extends Process {
    int noOfPumps;
    int noOfCashiers;
    double simPeriod = 300;
    Head availablePumps = new Head();
    Head availableCashiers = new Head(); 
    Head waitingCars = new Head();//fronta čekajících aut v čerpací stanici
    Head waitingForPayment = new Head();//fronta čekající na placení
    Random random = new Random(5);
    double totalServiceTime;
    int noOfServedCars, maxQueueLength;
    long startTime = System.currentTimeMillis();

    GasStationSimulation(int n, int m) { noOfPumps = n; noOfCashiers=m;}

    public void actions() {
        for (int i = 0; i < noOfPumps; i++)
            new FuelPump().into(availablePumps);
        for (int i = 0; i < noOfCashiers; i++)
            new Cashier().into(availableCashiers);
        activate(new CarGenerator());
        hold(simPeriod + 1000000);
        report();
    }

    void report() {
        System.out.println(noOfPumps + " fuel pump simulation");
        System.out.println(noOfCashiers + " cashiers simulation");
        System.out.println("Number of cars served = " + noOfServedCars);
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance();
        fmt.setMaximumFractionDigits(2);
        System.out.println("Average service time = " + fmt.format(totalServiceTime / noOfServedCars));
        System.out.println("Maximum queue length = " + maxQueueLength);
        System.out.println("\nExecution time: " + fmt.format((System.currentTimeMillis() - startTime) / 1000.0) + " secs.\n");
    }

    class Car extends Process {
        private double fuelLevel;
        //konstruktor pro vyvtáření objektu Car() s náhodným vygenerováním aktuálního množství paliva na základě normálního rozdělení
        public Car(){
            Random rand = new Random();
            double mean = 30;
            double std = 10;
            double fuelLevel = rand.nextGaussian() *std + mean;
            this.fuelLevel = Math.max(0, Math.min(fuelLevel, 100)); //omezení kvůli předpokladu množství paliva v procentech
        };
        public double getCarFuelLevel(){
            return this.fuelLevel;
        };
        public void actions() {
            double arrivalTime = time();
            into(waitingCars);
            while (true) {
                if (!availablePumps.empty()) {
                    activate((FuelPump) availablePumps.first());
                }
                passivate(); // Auto čeká na dokončení tankování
                
                // Po dokončení tankování auto přejde k placení
                into(waitingForPayment);
                if (!availableCashiers.empty()) {
                    activate((Cashier) availableCashiers.first());
                }
                passivate(); // Auto čeká na dokončení placení
                break; // Po dokončení placení je proces auta ukončen
            }
            totalServiceTime += time() - arrivalTime;
            noOfServedCars++;
            out(); // Odstranění auta z procesu
        }
        //metoda pro placení
        public void startPayment() {
        into(waitingForPayment);
        if (!availableCashiers.empty()) {
            activate((Cashier) availableCashiers.first());
        }
        passivate(); // Auto čeká, dokud nebude placení dokončeno
    }
    }

    class FuelPump extends Process {
        public void actions() {
            while (true) {
                if (!waitingCars.empty()) {
                    Car car = (Car) waitingCars.first();
                    car.out(); // Odstranění auta z fronty čekajících na tankování
                    double fuelNeeded = 100 - car.getCarFuelLevel();
                    double refuelingTime = calculateRefuelingTime(fuelNeeded);
                    hold(refuelingTime); // Simulace doby tankování
                    activate(car); // Přesun auta k placení
                }
                passivate(); // Pumpa čeká na další auto
            }
        }
        
        // Metoda pro výpočet doby tankování - předpoklad, že existuje přímá závislost mezi chybějícím palivem a dobou tankování
        // předpokladem je, že natankování 100% nádrže trvá 10 minut
        private double calculateRefuelingTime(double fuelNeeded) {
            return fuelNeeded / 10;
        }
    }

    class Cashier extends Process {
        public void actions() {
            while (true) {
                if (!waitingForPayment.empty()) {
                    Car car = (Car) waitingForPayment.first();
                    car.out(); // Odstranění auta z fronty čekajících na placení
                    double paymentTime = calculatePaymentTime();
                    hold(paymentTime); // Simulace doby placení
                    activate(car);
                }
                passivate();
                //wait(availableCashiers);
            }
        }

        private double calculatePaymentTime() {
            double mean = 2;
            double std = 1;
            return Math.abs(random.nextGaussian() * std + mean);
        }
    }

    class CarGenerator extends Process {
        public void actions() {
            while (time() <= simPeriod) {
                //activate(new Car());
                new Car().into(waitingCars);
                hold(random.negexp(1 / 10.0));
            }
        }
    }
    public static void main(String args[]) {
        activate(new GasStationSimulation(1,1));
        activate(new GasStationSimulation(2,2));
        activate(new GasStationSimulation(4,4));
    } 
}
