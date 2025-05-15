package org.cloudbus.cloudsim.examples;

import java.util.Locale;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.io.IOException;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimple;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.examples.ABC;

public class CloudSimulation2 {

    private static PowerDatacenter datacenter1, datacenter2, datacenter3, datacenter4, datacenter5, datacenter6;
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;
    private static int bot = 10;
    private static FileWriter csvWriter;
    private static final int NUM_TRIALS = 10;
    private static final Random random = new Random();

    public static void main(String[] args) {
        Locale.setDefault(new Locale("en", "US"));
        Log.printLine("Starting Cloud Simulation with ABC Algorithm...");

        try {
            // Create CSV file for results
            csvWriter = new FileWriter("ABC_Results.csv");
            csvWriter.write("Dataset,Trial,Min Response Time,Response Time,Total CPU Time,Total Wait Time," +
                          "Total Cloudlets Finished,Average Cloudlets Finished,Average Start Time," +
                          "Average Execution Time,Average Finish Time,Average Waiting Time,Throughput," +
                          "Makespan,Imbalance Degree,Total Scheduling Length,Resource Utilization," +
                          "Total Energy Consumption\n");

            // Run simulations for SDSC dataset only
            String dataset = "SDSC7395";
            System.out.println("\n=== Running simulations for dataset: " + dataset + " ===");
            System.out.println("Running " + NUM_TRIALS + " trials for statistical significance");
            
            for (int trial = 1; trial <= NUM_TRIALS; trial++) {
                System.out.println("\nTrial " + trial + " of " + NUM_TRIALS);
                runSimulation(dataset, trial);
            }

            csvWriter.close();
            System.out.println("\nAll simulations completed. Results saved to ABC_Results.csv");
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Simulation terminated due to an error");
        }
    }

    private static void runSimulation(String dataset, int trial) {
        Locale.setDefault(new Locale("en", "US"));
        Log.printLine("Starting Cloud Simulation with ABC Algorithm...");

        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            int hostId = 0;

            datacenter1 = createDatacenter("DataCenter_1", hostId);
            hostId = 3;
            datacenter2 = createDatacenter("DataCenter_2", hostId);
            hostId = 6;
            datacenter3 = createDatacenter("DataCenter_3", hostId);
            hostId = 9;
            datacenter4 = createDatacenter("DataCenter_4", hostId);
            hostId = 12;
            datacenter5 = createDatacenter("DataCenter_5", hostId);
            hostId = 15;
            datacenter6 = createDatacenter("DataCenter_6", hostId);

            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();
            int vmNumber = 54;
            int cloudletNumber = 7395; // SDSC dataset size

            vmlist = createVM(brokerId, vmNumber);
            cloudletList = createCloudlet(brokerId, cloudletNumber, dataset);

            broker.submitVmList(vmlist);
            broker.submitCloudletList(cloudletList);

            // Run ABC algorithm
            runABCAlgorithm(cloudletList, vmlist);

            System.out.println("Starting the simulation...");
            CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // Save results to CSV
            saveResultsToCSV(dataset, trial, newList);

            Log.printLine("Cloud Simulation with ABC finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Simulation terminated due to an error");
        }
    }

    private static void runABCAlgorithm(List<Cloudlet> cloudlets, List<Vm> vms) {
        System.out.println("\n=== ABC Algorithm Parameters ===");
        
        try {
            // ABC Algorithm Parameters - Standard ABC configuration
            int numberOfBees = 30;            // Total swarm size
            int limit = 5;                   // Limit before abandonment
            int maxIterations = 50;           // Number of iterations
            
            System.out.println("Problem Size:");
            System.out.println("Total Cloudlets: " + cloudlets.size());
            System.out.println("Total VMs: " + vms.size());
            
            System.out.println("\nABC Parameters:");
            System.out.println("Total Swarm Size: " + numberOfBees);
            System.out.println("Employed Bees: " + (numberOfBees/2));
            System.out.println("Onlooker Bees: " + (numberOfBees/2));
            System.out.println("Scout Bees: 1");
            System.out.println("Abandonment Limit: " + limit);
            System.out.println("Max Iterations: " + maxIterations);
            
            System.out.println("\n=== Starting ABC Optimization ===");
            System.out.println("Initializing bee population...");
            
            // Create and run ABC algorithm with standard parameters
            ABC abc = new ABC(
                cloudlets,
                vms,
                numberOfBees,
                limit,
                maxIterations
            );
            
            System.out.println("Running ABC algorithm for " + maxIterations + " iterations...");
            
            // Run the algorithm and get the best solution
            long startTime = System.currentTimeMillis();
            int[] bestSolution = abc.run();
            long endTime = System.currentTimeMillis();
            
            // Calculate runtime
            double runtimeInSeconds = (endTime - startTime) / 1000.0;
            
            // Apply the best solution to cloudlets
            for (int i = 0; i < cloudlets.size(); i++) {
                cloudlets.get(i).setVmId(bestSolution[i]);
            }
            
            // Output VM distribution in the best solution
            int[] vmAssignments = new int[vms.size()];
            for (int vmId : bestSolution) {
                vmAssignments[vmId]++;
            }
            
            System.out.println("\n=== ABC Optimization Results ===");
            System.out.println("Algorithm completed in " + runtimeInSeconds + " seconds");
            System.out.println("Best fitness: " + abc.getGlobalBestFitness());
            System.out.println("Best makespan: " + abc.getBestMakespan());
            
            System.out.println("\nVM Load Distribution in Best Solution:");
            for (int i = 0; i < vms.size(); i++) {
                System.out.println("VM " + i + ": " + vmAssignments[i] + " cloudlets");
            }
            
            // Calculate load balance metric
            double avgLoad = (double) cloudlets.size() / vms.size();
            double variance = 0;
            for (int load : vmAssignments) {
                variance += Math.pow(load - avgLoad, 2);
            }
            variance /= vms.size();
            
            System.out.println("\nLoad Balance Metrics:");
            System.out.println("Average Load per VM: " + avgLoad);
            System.out.println("Load Variance: " + variance);
            System.out.println("Load Standard Deviation: " + Math.sqrt(variance));
            
            System.out.println("\nABC Optimization completed successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("ABC Algorithm terminated due to an error");
        }
    }

    // Method to create Cloudlets
    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, String dataset) {
        ArrayList<Double> randomSeed = getSeedValue(cloudlets, dataset);

        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < cloudlets; i++) {
            long length = 0;

            if (randomSeed.size() > i) {
                length = Double.valueOf(randomSeed.get(i)).longValue();
            }

            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(userId);
            list.add(cloudlet);
        }
        Collections.shuffle(list);

        return list;
    }

    // Method to create VMs
    private static List<Vm> createVM(int userId, int vms) {
        LinkedList<Vm> list = new LinkedList<Vm>();

        long size = 10000;
        int[] ram = { 512, 1024, 2048 };
        int[] mips = { 400, 500, 600 };
        long bw = 1000;
        int pesNumber = 1;
        String vmm = "Xen";

        Vm[] vm = new Vm[vms];

        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(i, userId, mips[i % 3], pesNumber, ram[i % 3], bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }

        return list;
    }

    // Method to get random seed value
    private static ArrayList<Double> getSeedValue(int cloudletcount, String dataset) {
        ArrayList<Double> seed = new ArrayList<Double>();
        try {
            String filePath = System.getProperty("user.dir") + "/cloudsim-3.0.3/datasets/";
            if (dataset.startsWith("RandSimple")) {
                filePath += "randomSimple/" + dataset + ".txt";
            } else if (dataset.startsWith("RandStratified")) {
                filePath += "randomStratified/" + dataset + ".txt";
            } else {
                filePath += "SDSC/" + dataset + ".txt";
            }
            
            File fobj = new File(filePath);
            java.util.Scanner readFile = new java.util.Scanner(fobj);

            while (readFile.hasNextLine() && cloudletcount > 0) {
                seed.add(readFile.nextDouble());
                cloudletcount--;
            }
            readFile.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return seed;
    }

    // Method to create Datacenter
    private static PowerDatacenter createDatacenter(String name, int hostId) {

        List<PowerHost> hostList = new ArrayList<PowerHost>();

        List<Pe> peList1 = new ArrayList<Pe>();
        List<Pe> peList2 = new ArrayList<Pe>();
        List<Pe> peList3 = new ArrayList<Pe>();

        int mipsunused = 300; 
        int mips1 = 400; 
        int mips2 = 500;
        int mips3 = 600;

        peList1.add(new Pe(0, new PeProvisionerSimple(mips1))); 
        peList1.add(new Pe(1, new PeProvisionerSimple(mips1)));
        peList1.add(new Pe(2, new PeProvisionerSimple(mips1)));
        peList1.add(new Pe(3, new PeProvisionerSimple(mipsunused)));
        peList2.add(new Pe(4, new PeProvisionerSimple(mips2)));
        peList2.add(new Pe(5, new PeProvisionerSimple(mips2)));
        peList2.add(new Pe(6, new PeProvisionerSimple(mips2)));
        peList2.add(new Pe(7, new PeProvisionerSimple(mipsunused)));
        peList3.add(new Pe(8, new PeProvisionerSimple(mips3)));
        peList3.add(new Pe(9, new PeProvisionerSimple(mips3)));
        peList3.add(new Pe(10, new PeProvisionerSimple(mips3)));
        peList3.add(new Pe(11, new PeProvisionerSimple(mipsunused)));

        int ram = 128000;
        long storage = 1000000;
        int bw = 10000;
        int maxpower = 117; 
        int staticPowerPercentage = 50; 

        hostList.add(
            new PowerHostUtilizationHistory(
                hostId, new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList1,
                new VmSchedulerTimeShared(peList1),
                new PowerModelLinear(maxpower, staticPowerPercentage)));
        hostId++;

        hostList.add(
            new PowerHostUtilizationHistory(
                hostId, new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList2,
                new VmSchedulerTimeShared(peList2),
                new PowerModelLinear(maxpower, staticPowerPercentage)));
        hostId++;

        hostList.add(
            new PowerHostUtilizationHistory(
                hostId, new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList3,
                new VmSchedulerTimeShared(peList3),
                new PowerModelLinear(maxpower, staticPowerPercentage)));

        String arch = "x86"; 
        String os = "Linux"; 
        String vmm = "Xen"; 
        double time_zone = 10.0; 
        double cost = 3.0; 
        double costPerMem = 0.05; 
        double costPerStorage = 0.1; 
        double costPerBw = 0.1; 
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        PowerDatacenter datacenter = null;
        try {
          datacenter = new PowerDatacenter(name, characteristics, new PowerVmAllocationPolicySimple(hostList), storageList, 9); 
        } catch (Exception e) {
          e.printStackTrace();
        }

        return datacenter;
      }

    // Method to create Broker
    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    // Print Cloudlet List with detailed metrics
    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet = null;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
            "Data center ID" + indent + "VM ID" + indent + "Time"
            + indent + "Start Time" + indent + "Finish Time" + indent + "Waiting Time");

        double waitTimeSum = 0.0;
        double CPUTimeSum = 0.0;
        int totalValues = 0;
        DecimalFormat dft = new DecimalFormat("###,##");

        double response_time[] = new double[size];

        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");
                CPUTimeSum = CPUTimeSum + cloudlet.getActualCPUTime();
                waitTimeSum = waitTimeSum + cloudlet.getWaitingTime();
                Log.printLine(
                    indent + indent + indent + (cloudlet.getResourceId() - 1) + indent + indent + indent + cloudlet.getVmId() +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent
                        + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + dft.format(cloudlet.getFinishTime()) + indent + indent + indent
                        + dft.format(cloudlet.getWaitingTime()));
                totalValues++;

                response_time[i] = cloudlet.getActualCPUTime();
            }
        }
        DoubleSummaryStatistics stats = DoubleStream.of(response_time).summaryStatistics();

        Log.printLine();
        System.out.println(String.format("min = %,6f",stats.getMin()));
        System.out.println(String.format("Response_Time: %,6f",CPUTimeSum / totalValues));

        Log.printLine();
        Log.printLine(String.format("TotalCPUTime : %,6f",CPUTimeSum));
        Log.printLine("TotalWaitTime : "+waitTimeSum);
        Log.printLine("TotalCloudletsFinished : "+totalValues);

        // Average Cloudlets Finished
        Log.printLine(String.format("AverageCloudletsFinished : %,6f",(CPUTimeSum / totalValues)));

        // Average Start Time
        double totalStartTime = 0.0;
        for (int i = 0; i < size; i++) {
            totalStartTime += cloudletList.get(i).getExecStartTime();
        }
        double avgStartTime = totalStartTime / size;
        System.out.println(String.format("Average StartTime: %,6f",avgStartTime));

        // Average Execution Time
        double ExecTime = 0.0;
        for (int i = 0; i < size; i++) {
            ExecTime += cloudletList.get(i).getActualCPUTime();
        }
        double avgExecTime = ExecTime / size;
        System.out.println(String.format("Average Execution Time: %,6f",avgExecTime));

        // Average Finish Time
        double totalTime = 0.0;
        for (int i = 0; i < size; i++) {
            totalTime += cloudletList.get(i).getFinishTime();
        }
        double avgTAT = totalTime / size;
        System.out.println(String.format("Average FinishTime: %,6f",avgTAT));

        // Average Waiting Time
        double avgWT = cloudlet.getWaitingTime() / size;
        System.out.println(String.format("Average Waiting time: %,6f",avgWT));

        // Throughput
        double maxFT = 0.0;
        for (int i = 0; i < size; i++) {
            double currentFT = cloudletList.get(i).getFinishTime();
            if (currentFT > maxFT) {
                maxFT = currentFT;
            }
        }
        double throughput = size / maxFT;
        System.out.println(String.format("Throughput: %,9f",throughput));

        // Makespan
        double makespan = 0.0;
        double makespan_total = makespan + cloudlet.getFinishTime();
        System.out.println(String.format("Makespan: %,f",makespan_total));

        // Imbalance Degree
        double degree_of_imbalance = (stats.getMax() - stats.getMin()) / (CPUTimeSum / totalValues);
        System.out.println(String.format("Imbalance Degree: %,3f",degree_of_imbalance));

        // Scheduling Length
        double scheduling_length = waitTimeSum + makespan_total;
        Log.printLine(String.format("Total Scheduling Length: %,f", scheduling_length));

        // CPU Resource Utilization
        double resource_utilization = (CPUTimeSum / (makespan_total * 54)) * 100;
        Log.printLine(String.format("Resouce Utilization: %,f",resource_utilization));

        // Energy Consumption
        Log.printLine(String.format("Total Energy Consumption: %,2f  kWh",
            (datacenter1.getPower() + datacenter2.getPower() + datacenter3.getPower() + datacenter4.getPower()
                + datacenter5.getPower() + datacenter6.getPower()) / (3600 * 1000)));
    }

    private static void saveResultsToCSV(String dataset, int trial, List<Cloudlet> list) throws IOException {
        int size = list.size();
        Cloudlet cloudlet = null;

        double waitTimeSum = 0.0;
        double CPUTimeSum = 0.0;
        int totalValues = 0;
        double response_time[] = new double[size];

        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                CPUTimeSum = CPUTimeSum + cloudlet.getActualCPUTime();
                waitTimeSum = waitTimeSum + cloudlet.getWaitingTime();
                totalValues++;
                response_time[i] = cloudlet.getActualCPUTime();
            }
        }

        DoubleSummaryStatistics stats = DoubleStream.of(response_time).summaryStatistics();

        double totalStartTime = 0.0;
        for (int i = 0; i < size; i++) {
            totalStartTime += cloudletList.get(i).getExecStartTime();
        }
        double avgStartTime = totalStartTime / size;

        double ExecTime = 0.0;
        for (int i = 0; i < size; i++) {
            ExecTime += cloudletList.get(i).getActualCPUTime();
        }
        double avgExecTime = ExecTime / size;

        double totalTime = 0.0;
        for (int i = 0; i < size; i++) {
            totalTime += cloudletList.get(i).getFinishTime();
        }
        double avgTAT = totalTime / size;

        double avgWT = cloudlet.getWaitingTime() / size;

        double maxFT = 0.0;
        for (int i = 0; i < size; i++) {
            double currentFT = cloudletList.get(i).getFinishTime();
            if (currentFT > maxFT) {
                maxFT = currentFT;
            }
        }
        double throughput = size / maxFT;

        double makespan = 0.0;
        double makespan_total = makespan + cloudlet.getFinishTime();

        double degree_of_imbalance = (stats.getMax() - stats.getMin()) / (CPUTimeSum / totalValues);

        double scheduling_length = waitTimeSum + makespan_total;

        double resource_utilization = (CPUTimeSum / (makespan_total * 54)) * 100;

        double totalEnergy = (datacenter1.getPower() + datacenter2.getPower() + datacenter3.getPower() + 
                            datacenter4.getPower() + datacenter5.getPower() + datacenter6.getPower()) / (3600 * 1000);

        // Write results to CSV
        csvWriter.write(String.format("%s,%d,%.6f,%.6f,%.6f,%.6f,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.9f,%.6f,%.3f,%.6f,%.6f,%.2f\n",
            dataset, trial, stats.getMin(), CPUTimeSum / totalValues, CPUTimeSum, waitTimeSum,
            totalValues, CPUTimeSum / totalValues, avgStartTime, avgExecTime, avgTAT, avgWT,
            throughput, makespan_total, degree_of_imbalance, scheduling_length, resource_utilization, totalEnergy));
        csvWriter.flush();
    }
}