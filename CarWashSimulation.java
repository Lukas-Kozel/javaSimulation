import javaSimulation.*;
import javaSimulation.Process;

public class CarWashSimulation extends Process {
    int noOfCarWashers;
    double simPeriod = 200; 
    Head tearoom = new Head();
    Head waitingLine = new Head();
    Random random = new Random(5);
    double throughTime; 
    int noOfCustomers, maxLength;
    long startTime = System.currentTimeMillis();
	
    CarWashSimulation(int n) { noOfCarWashers = n; }

    public void actions() { 
        for (int i = 1; i <= noOfCarWashers; i++)
            new CarWasher().into(tearoom);
        activate(new CarGenerator());
        hold(simPeriod + 1000000);	
        report();
    }
	
    void report() {
        System.out.println(noOfCarWashers + " car washer simulation");
        System.out.println("No.of cars through the system = " + 
                           noOfCustomers);
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance();
        fmt.setMaximumFractionDigits(2);
        System.out.println("Av.elapsed time = " + 
                           fmt.format(throughTime/noOfCustomers));
        System.out.println("Maximum queue length = " + maxLength);
        System.out.println("\nExecution time: " +
                           fmt.format((System.currentTimeMillis() 
                                       - startTime)/1000.0) +			   
                           " secs.\n");
    }
	
    class Car extends Process {
        public void actions() {
            double entryTime = time();
            into(waitingLine);
            int qLength = waitingLine.cardinal();
            if (maxLength < qLength) 
                maxLength = qLength;
            if (!tearoom.empty()) 
                activate((CarWasher) tearoom.first());
            passivate();
            noOfCustomers++;
            throughTime += time() - entryTime;
        }		
    }

    class CarWasher extends Process {
        public void actions() { 
            while (true) { 
                out();
                while (!waitingLine.empty()) {
                    Car served = (Car) waitingLine.first();
                    served.out();
                    hold(10);
                    activate(served);
                }
                wait(tearoom);
            }
        }
    }

    class CarGenerator extends Process {
        public void actions() {
             while (time() <= simPeriod) {
                  activate(new Car());
                  hold(random.negexp(1/11.0));
             }
        }
    }

    public static void main(String args[]) {
        activate(new CarWashSimulation(1));
        activate(new CarWashSimulation(2));
    } 
}
