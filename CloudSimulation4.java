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

public class CloudSimulation4 {

    private static PowerDatacenter datacenter1, datacenter2, datacenter3, datacenter4, datacenter5, datacenter6;
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;
    private static FileWriter csvWriter;
    private static final int NUM_TRIALS = 10;
    private static final Random random = new Random();

    public static void main(String[] args) {
        Locale.setDefault(new Locale("en", "US"));
        Log.printLine("Starting Cloud Simulation with ABC-EOBL Algorithm...");

        try {
            // Create CSV file for results
            csvWriter = new FileWriter("ABC_EOBL_Results.csv");
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
            System.out.println("\nAll simulations completed. Results saved to ABC_EOBL_Results.csv");
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Simulation terminated due to an error");
        }
    }

    private static void runSimulation(String dataset, int trial) {
        Locale.setDefault(new Locale("en", "US"));
        Log.printLine("Starting Cloud Simulation with ABC-EOBL Algorithm...");

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

            // Run ABC-EOBL algorithm
            runABCEOBLAlgorithm(cloudletList, vmlist);

            System.out.println("Starting the simulation...");
            CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // Save results to CSV
            saveResultsToCSV(dataset, trial, newList);

            Log.printLine("Cloud Simulation with ABC-EOBL finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Simulation terminated due to an error");
        }
    }

    private static void runABCEOBLAlgorithm(List<Cloudlet> cloudlets, List<Vm> vms) {
        System.out.println("\n=== ABC-EOBL Algorithm Parameters ===");
        
        try {
            // Step 1: Initialize
            int totalCloudlets = cloudlets.size();
            int totalVMs = vms.size();
            
            // ABC-EOBL Parameters
            int numberOfBees = 100;
            int numberOfScout = 15;
            int numberOfInactive = 25;
            int numberOfActive = numberOfBees - (numberOfInactive + numberOfScout);
            int maxVisits = 15;
            double probMistake = 0.2;    // Probability of making mistake
            double probPersuasion = 0.6;  // Probability of persuasion for inactive bees
            double probEOBL = 0.3;        // Probability of applying EOBL
            int tmax = 25;                // Maximum iterations
            
            int currentIteration = 1;
            
            System.out.println("Problem Size:");
            System.out.println("Total Cloudlets: " + totalCloudlets);
            System.out.println("Total VMs: " + totalVMs);
            
            System.out.println("\nABC-EOBL Parameters:");
            System.out.println("Number of Bees: " + numberOfBees);
            System.out.println("Active Bees: " + numberOfActive);
            System.out.println("Inactive Bees: " + numberOfInactive);
            System.out.println("Scout Bees: " + numberOfScout);
            System.out.println("Max Visits: " + maxVisits);
            System.out.println("Probability of Mistake: " + probMistake);
            System.out.println("Probability of Persuasion: " + probPersuasion);
            System.out.println("Probability of EOBL: " + probEOBL);
            System.out.println("Max Iterations: " + tmax);
            
            // Initialize population and solutions
            List<int[]> population = new ArrayList<>();
            double[] fitness = new double[numberOfBees];
            int[] visits = new int[numberOfBees];
            int[] beeTypes = new int[numberOfBees]; // 0=inactive, 1=active, 2=scout
            
            // Step 2: Generate Random Solution for each bee
            for (int i = 0; i < numberOfBees; i++) {
                int[] solution = new int[totalCloudlets];
                for (int j = 0; j < totalCloudlets; j++) {
                    solution[j] = random.nextInt(totalVMs);
                }
                population.add(solution);
                
                // Set bee types
                if (i < numberOfActive) {
                    beeTypes[i] = 1; // Active
                } else if (i < numberOfActive + numberOfInactive) {
                    beeTypes[i] = 0; // Inactive
                } else {
                    beeTypes[i] = 2; // Scout
                }
                
                // Initialize visits
                visits[i] = 0;
                
                // Calculate initial fitness
                fitness[i] = calculateFitness(solution, cloudlets, vms);
            }
            
            // Apply EOBL to initial population - Create opposition-based solutions for a subset
            applyEOBLToInitialPopulation(population, fitness, totalVMs, cloudlets, vms, probEOBL);
            
            // Step 3: Update Current Best Solution
            int[] globalBestSolution = null;
            double globalBestFitness = Double.MAX_VALUE;
            
            // Find initial best solution
            for (int i = 0; i < numberOfBees; i++) {
                if (fitness[i] < globalBestFitness) {
                    globalBestFitness = fitness[i];
                    globalBestSolution = population.get(i).clone();
            }
            }
            
            System.out.println("Initial best fitness: " + globalBestFitness);
            
            // Main loop
            while (currentIteration <= tmax) {
                System.out.println("\nIteration " + currentIteration + " of " + tmax);
                
                // Step 4: Process each bee
                for (int k = 0; k < numberOfBees; k++) {
                    if (beeTypes[k] == 1) { // Active bee
                        processActiveBee(k, population, fitness, visits, beeTypes, probMistake, maxVisits, totalVMs, totalCloudlets, cloudlets, vms, probEOBL);
                    } else if (beeTypes[k] == 2) { // Scout bee
                        processScoutBee(k, population, fitness, totalVMs, totalCloudlets, cloudlets, vms, probEOBL);
                    } else { // Inactive bee
                        processInactiveBee(k, population, fitness, beeTypes, probPersuasion, cloudlets, vms);
                    }
                }
                
                // Update global best solution
                for (int i = 0; i < numberOfBees; i++) {
                    if (fitness[i] < globalBestFitness) {
                        globalBestFitness = fitness[i];
                        globalBestSolution = population.get(i).clone();
                        System.out.println("New best fitness: " + globalBestFitness);
                    }
                }
                
                // Step 5: Increment iteration
                currentIteration++;
            }
            
            // Apply the best solution to cloudlets
            System.out.println("\nApplying best solution with fitness: " + globalBestFitness);
            for (int i = 0; i < totalCloudlets; i++) {
                cloudlets.get(i).setVmId(globalBestSolution[i]);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("ABC-EOBL Algorithm terminated due to an error");
        }
    }
    
    // Apply EOBL to initial population
    private static void applyEOBLToInitialPopulation(List<int[]> population, double[] fitness, int totalVMs, 
                                                    List<Cloudlet> cloudlets, List<Vm> vms, double probEOBL) {
        int populationSize = population.size();
        
        for (int i = 0; i < populationSize; i++) {
            if (random.nextDouble() < probEOBL) {
                int[] currentSolution = population.get(i);
                int[] oppositionSolution = generateOppositionSolution(currentSolution, totalVMs);
                
                double oppositionFitness = calculateFitness(oppositionSolution, cloudlets, vms);
                
                // Replace with opposition solution if it's better
                if (oppositionFitness < fitness[i]) {
                    population.set(i, oppositionSolution);
                    fitness[i] = oppositionFitness;
        }
    }
        }
    }
    
    // Generate opposition-based solution
    private static int[] generateOppositionSolution(int[] current, int totalVMs) {
        int[] opposition = new int[current.length];
        for (int i = 0; i < current.length; i++) {
            opposition[i] = totalVMs - 1 - current[i];
        }
        return opposition;
    }

    // Process active bees
    private static void processActiveBee(int beeIndex, List<int[]> population, double[] fitness, int[] visits, 
                                       int[] beeTypes, double probMistake, int maxVisits, int totalVMs, 
                                       int totalCloudlets, List<Cloudlet> cloudlets, List<Vm> vms, double probEOBL) {
        int[] currentSolution = population.get(beeIndex);
        
        // Generate neighbor solution
        int[] neighborSolution = generateNeighborSolution(currentSolution, totalVMs);
        
        // Apply EOBL with probability
        if (random.nextDouble() < probEOBL) {
            int[] oppositionSolution = generateOppositionSolution(neighborSolution, totalVMs);
            double neighborFitness = calculateFitness(neighborSolution, cloudlets, vms);
            double oppositionFitness = calculateFitness(oppositionSolution, cloudlets, vms);
            
            // Keep the better solution between neighbor and opposition
            if (oppositionFitness < neighborFitness) {
                neighborSolution = oppositionSolution;
            }
        }
        
        // Calculate fitness values
        double currentFitness = fitness[beeIndex];
        double neighborFitness = calculateFitness(neighborSolution, cloudlets, vms);
        
        // Make decision based on fitness and probability
        if (neighborFitness < currentFitness) {
            if (random.nextDouble() < probMistake) {
                // Mistakenly keep the worse solution
                visits[beeIndex]++;
            } else {
                // Replace with better solution
                population.set(beeIndex, neighborSolution);
                fitness[beeIndex] = neighborFitness;
                visits[beeIndex] = 0;
            }
        } else {
            if (random.nextDouble() < probMistake) {
                // Mistakenly accept worse solution
                population.set(beeIndex, neighborSolution);
                fitness[beeIndex] = neighborFitness;
                visits[beeIndex] = 0;
            } else {
                // Keep current solution
                visits[beeIndex]++;
            }
        }
        
        // Check if bee should become scout due to limit exceeded
        if (visits[beeIndex] > maxVisits) {
            // Find an inactive bee to swap with
            for (int i = 0; i < beeTypes.length; i++) {
                if (beeTypes[i] == 0) { // Inactive bee
                    // Swap solutions
                    int[] temp = population.get(beeIndex).clone();
                    population.set(beeIndex, population.get(i).clone());
                    population.set(i, temp);
                    
                    // Swap fitness
                    double tempFitness = fitness[beeIndex];
                    fitness[beeIndex] = fitness[i];
                    fitness[i] = tempFitness;
                    
                    // Reset visits
                    visits[beeIndex] = 0;
                    break;
                }
            }
        }
    }
    
    // Process scout bees
    private static void processScoutBee(int beeIndex, List<int[]> population, double[] fitness, 
                                     int totalVMs, int totalCloudlets, List<Cloudlet> cloudlets, 
                                     List<Vm> vms, double probEOBL) {
        // Generate a completely new random solution
        int[] newSolution = new int[totalCloudlets];
        for (int i = 0; i < totalCloudlets; i++) {
            newSolution[i] = random.nextInt(totalVMs);
        }
        
        // Apply EOBL with probability
        if (random.nextDouble() < probEOBL) {
            int[] oppositionSolution = generateOppositionSolution(newSolution, totalVMs);
            double newFitness = calculateFitness(newSolution, cloudlets, vms);
            double oppositionFitness = calculateFitness(oppositionSolution, cloudlets, vms);
            
            // Keep the better solution
            if (oppositionFitness < newFitness) {
                newSolution = oppositionSolution;
                fitness[beeIndex] = oppositionFitness;
            } else {
                fitness[beeIndex] = newFitness;
            }
        } else {
            fitness[beeIndex] = calculateFitness(newSolution, cloudlets, vms);
        }
        
        // Update solution
        population.set(beeIndex, newSolution);
    }
    
    // Process inactive bees
    private static void processInactiveBee(int beeIndex, List<int[]> population, double[] fitness,
                                       int[] beeTypes, double probPersuasion, List<Cloudlet> cloudlets, 
                                       List<Vm> vms) {
        // Find the best active bee
        int bestActiveBee = -1;
        double bestFitness = Double.MAX_VALUE;
        
        for (int i = 0; i < beeTypes.length; i++) {
            if (beeTypes[i] == 1) { // Active bee
                if (fitness[i] < bestFitness) {
                    bestFitness = fitness[i];
                    bestActiveBee = i;
                }
            }
        }
        
        if (bestActiveBee != -1 && random.nextDouble() < probPersuasion) {
            // Copy the solution from the best active bee with small modification
            int[] bestSolution = population.get(bestActiveBee);
            int[] newSolution = bestSolution.clone();
            
            // Make small change to avoid exact copying
            if (newSolution.length > 0) {
                int pos = random.nextInt(newSolution.length);
                int randomVM = random.nextInt(vms.size());
                newSolution[pos] = randomVM;
            }
            
            // Update solution and fitness
            population.set(beeIndex, newSolution);
            fitness[beeIndex] = calculateFitness(newSolution, cloudlets, vms);
        }
    }
    
    // Generate neighbor solution
    private static int[] generateNeighborSolution(int[] current, int totalVMs) {
        int[] neighbor = current.clone();
        
        // Change multiple positions to create better neighborhood (can tune this)
        int numChanges = 1 + random.nextInt(3); // 1-3 changes
        for (int i = 0; i < numChanges; i++) {
            if (neighbor.length > 0) {
        int pos = random.nextInt(neighbor.length);
        neighbor[pos] = random.nextInt(totalVMs);
            }
        }
        
        return neighbor;
    }

    private static double calculateFitness(int[] solution, List<Cloudlet> cloudlets, List<Vm> vms) {
        double[] vmLoads = new double[vms.size()];
        double[] processingTimes = new double[vms.size()];
        
        // Calculate load and processing time for each VM
        for (int i = 0; i < cloudlets.size(); i++) {
            int vmIndex = solution[i];
            
            if (vmIndex >= vms.size()) {
                return Double.MAX_VALUE; // Invalid solution
            }
            
            double cloudletLength = cloudlets.get(i).getCloudletLength();
            double vmMips = vms.get(vmIndex).getMips();
            
            vmLoads[vmIndex] += cloudletLength;
            processingTimes[vmIndex] += cloudletLength / vmMips;
        }
        
        // Calculate makespan (maximum processing time on any VM)
        double makespan = 0;
        for (double time : processingTimes) {
            makespan = Math.max(makespan, time);
        }
        
        // Calculate load balance
        double avgLoad = 0;
        for (double load : vmLoads) {
            avgLoad += load;
    }
        avgLoad /= vms.size();
        
        double loadImbalance = 0;
        for (double load : vmLoads) {
            loadImbalance += Math.abs(load - avgLoad);
        }
        loadImbalance /= vms.size();
        
        // Consider both makespan and load balance in fitness
        // Lower fitness is better
        return makespan * (1 + 0.5 * loadImbalance / avgLoad);
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

    // Method to save results to CSV
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
        
        // Print summary
        System.out.println("\nTrial " + trial + " Results Summary:");
        System.out.println("Makespan: " + String.format("%.2f", makespan_total));
        System.out.println("Throughput: " + String.format("%.6f", throughput));
        System.out.println("Resource Utilization: " + String.format("%.2f%%", resource_utilization));
        System.out.println("Imbalance Degree: " + String.format("%.3f", degree_of_imbalance));
        System.out.println("Total Energy Consumption: " + String.format("%.2f kWh", totalEnergy));
    }
} 