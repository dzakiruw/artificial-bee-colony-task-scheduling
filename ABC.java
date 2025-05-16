package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Revised implementation of the Artificial Bee Colony (ABC) algorithm
 * with main algorithm logic in this class rather than in PopulationABC
 */
public class ABC {
    // Algorithm parameters
    private int numBees;         // Total swarm size
    private int numOnlookers;    // Number of onlooker bees
    private int numEmployed;     // Number of employed bees
    private int numScout;        // Number of scout bees
    private int limit;           // Limit of visits before abandonment
    private int maxIterations;   // Maximum number of iterations
    private int currentIteration; // Current iteration
    
    // EOBL parameters
    private boolean useEOBL;     // Whether to use EOBL
    private double probEOBL;     // Probability of applying EOBL
    private double jumpRate;     // EOBL jump rate
    
    // Problem data
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    private int cloudletCount;
    private int vmCount;
    
    // Algorithm state
    private PopulationABC population;
    private IndividualABC globalBest;
    private Random random;
    
    /**
     * Constructor for standard ABC algorithm without EOBL
     * 
     * @param cloudletList List of cloudlets to be scheduled
     * @param vmList List of VMs available
     * @param numBees Total number of bees in the swarm
     * @param limit Maximum number of visits before abandoning a food source
     * @param maxIterations Maximum number of iterations
     */
    public ABC(List<Cloudlet> cloudletList, List<Vm> vmList, int numBees, 
                int limit, int maxIterations) {
        this(cloudletList, vmList, numBees, limit, maxIterations, false, 0.0, 0.0);
    }
    
    /**
     * Constructor for ABC algorithm with EOBL enhancement
     * 
     * @param cloudletList List of cloudlets to be scheduled
     * @param vmList List of VMs available
     * @param numBees Total number of bees in the swarm
     * @param limit Maximum number of visits before abandoning a food source
     * @param maxIterations Maximum number of iterations
     * @param useEOBL Whether to use EOBL enhancement
     * @param probEOBL Probability of applying EOBL in each iteration
     * @param jumpRate EOBL jump rate (0.0-1.0)
     */
    public ABC(List<Cloudlet> cloudletList, List<Vm> vmList, int numBees, 
                int limit, int maxIterations, boolean useEOBL, double probEOBL, double jumpRate) {
        this.cloudletList = cloudletList;
        this.vmList = vmList;
        this.numBees = numBees;
        this.limit = limit;
        this.maxIterations = maxIterations;
        this.currentIteration = 0;
        this.cloudletCount = cloudletList.size();
        this.vmCount = vmList.size();
        this.random = new Random();
        
        // EOBL parameters
        this.useEOBL = useEOBL;
        this.probEOBL = probEOBL;
        this.jumpRate = jumpRate;
        
        // In standard ABC, the swarm is divided equally between employed and onlooker bees
        this.numEmployed = numBees / 2;
        this.numOnlookers = numBees / 2;
        this.numScout = 1;  // Standard ABC typically uses just 1 scout
        
        // Initialize population with structure but algorithm logic is here
        this.population = new PopulationABC(numBees, cloudletCount);
        this.globalBest = null;
    }
    
    /**
     * Run the ABC algorithm
     * 
     * @return The optimized mapping of cloudlets to VMs
     */
    public int[] run() {
        System.out.println("Initializing ABC algorithm...");
        
        // Initialize population
        initializePopulation();
        
        // Calculate initial fitness for all bees
        calculateFitnessForAllBees();
        
        // Apply EOBL to initial population if enabled
        if (useEOBL && random.nextDouble() < probEOBL) {
            applyEOBL();
        }
        
        // Main loop - follows standard ABC algorithm steps
        for (currentIteration = 1; currentIteration <= maxIterations; currentIteration++) {
            System.out.println("ABC Iteration: " + currentIteration + "/" + maxIterations);
            
            // Employed Bee Phase
            employedBeePhase();
            
            // Calculate probabilities for food sources
            double[] probabilities = calculateFoodSourceProbabilities();
            
            // Onlooker Bee Phase
            onlookerBeePhase(probabilities);
            
            // Scout Bee Phase
            scoutBeePhase();
            
            // Store best food source
            updateGlobalBest();
            
            // Apply EOBL enhancement if enabled
            if (useEOBL && random.nextDouble() < probEOBL) {
                applyEOBL();
            }
        }
        
        System.out.println("ABC optimization completed.");
        System.out.println("Best fitness: " + globalBest.getFitness());
        System.out.println("Best makespan: " + (1.0 / globalBest.getFitness()));
        
        // Return the best solution found
        return globalBest.getVmAllocation();
    }
    
    /**
     * Apply Elite Opposition-Based Learning (EOBL) to the population
     * EOBL creates opposition solutions based on the current best solution
     */
    private void applyEOBL() {
        if (globalBest == null) return;
        
        System.out.println("Applying EOBL enhancement...");
        int[] eliteSolution = globalBest.getVmAllocation();
        
        for (IndividualABC bee : population.getBees()) {
            // Skip elite bee itself (global best)
            if (bee.getFitness() == globalBest.getFitness()) continue;
            
            // Create opposition solution with probability
            if (random.nextDouble() < probEOBL) {
                int[] currentSolution = bee.getVmAllocation();
                int[] oppositionSolution = new int[cloudletCount];
                
                // Generate elite-based opposition solution
                for (int i = 0; i < cloudletCount; i++) {
                    if (random.nextDouble() < jumpRate) {
                        // Opposition: LB + UB - x
                        oppositionSolution[i] = 0 + (vmCount-1) - currentSolution[i];
                        
                        // Ensure valid range
                        if (oppositionSolution[i] < 0) oppositionSolution[i] = 0;
                        if (oppositionSolution[i] >= vmCount) oppositionSolution[i] = vmCount-1;
                        
                        // Mix with elite solution (key feature of EOBL)
                        if (random.nextDouble() < 0.5) {
                            oppositionSolution[i] = eliteSolution[i];
                        }
                    } else {
                        // Keep original assignment
                        oppositionSolution[i] = currentSolution[i];
                    }
                }
                
                // Evaluate opposition solution
                double oppositionFitness = calculateBeesFitness(oppositionSolution);
                
                // Apply if better
                if (oppositionFitness > bee.getFitness()) {
                    bee.setVmAllocation(oppositionSolution);
                    bee.setFitness(oppositionFitness);
                    bee.resetVisitCount();
                }
            }
        }
    }
    
    /**
     * Calculate fitness for a solution represented as VM allocation array
     * 
     * @param vmAllocation Array of VM allocations for cloudlets
     * @return Fitness value
     */
    private double calculateBeesFitness(int[] vmAllocation) {
        // Calculate completion time for each VM
        double[] vmCompletionTime = new double[vmList.size()];
        
        // For each cloudlet, add its execution time to the assigned VM
        for (int i = 0; i < cloudletList.size(); i++) {
            int vmId = vmAllocation[i];
            Cloudlet cloudlet = cloudletList.get(i);
            Vm vm = vmList.get(vmId);
            
            // Calculate cloudlet execution time on this VM
            double executionTime = (double) cloudlet.getCloudletLength() / vm.getMips();
            vmCompletionTime[vmId] += executionTime;
        }

        // Makespan is the maximum completion time across all VMs
        double makespan = 0;
        for (double time : vmCompletionTime) {
            if (time > makespan) {
                makespan = time;
            }
        }
        
        // Fitness is inverse of makespan (higher is better)
        return 1.0 / makespan;
    }
    
    /**
     * Initialize the population with random solutions
     */
    private void initializePopulation() {
        // Initialize all bees with random solutions
        List<IndividualABC> bees = population.getBees();
        
        for (int i = 0; i < bees.size(); i++) {
            IndividualABC bee = bees.get(i);
            
            // Set the type of bee
            if (i < numEmployed) {
                bee.setType(IndividualABC.EMPLOYED);
            } else if (i < numEmployed + numOnlookers) {
                bee.setType(IndividualABC.ONLOOKER);
            } else {
                bee.setType(IndividualABC.SCOUT);
            }
            
            // Generate random solution
            int[] solution = bee.getVmAllocation();
            for (int j = 0; j < cloudletCount; j++) {
                solution[j] = random.nextInt(vmCount);
            }
        }
    }
    
    /**
     * Calculate fitness for all bees in the population
     */
    private void calculateFitnessForAllBees() {
        for (IndividualABC bee : population.getBees()) {
            double fitness = calculateBeesFitness(bee.getVmAllocation());
            bee.setFitness(fitness);
            
            // Update global best if needed
            if (globalBest == null || fitness > globalBest.getFitness()) {
                if (globalBest == null) {
                    globalBest = new IndividualABC(-1, -1, cloudletCount);
                }
                globalBest.setVmAllocation(bee.getVmAllocation());
                globalBest.setFitness(fitness);
            }
        }
    }
    
    /**
     * Employed Bee Phase - find new food sources and apply greedy selection
     */
    private void employedBeePhase() {
        System.out.println("Employed Bee Phase");
        
        for (IndividualABC bee : population.getBees()) {
            if (bee.getType() == IndividualABC.EMPLOYED) {
                processEmployedBee(bee);
            }
        }
    }
    
    /**
     * Process an employed bee - finds a neighbor solution and applies greedy selection
     * 
     * @param bee The employed bee to process
     */
    private void processEmployedBee(IndividualABC bee) {
        // Store original solution and fitness
        int[] currentSolution = bee.getVmAllocation();
        double currentFitness = bee.getFitness();
        
        // Generate a neighbor solution
        int[] neighborSolution = currentSolution.clone();
        
        // Modify the solution by changing one cloudlet's VM assignment
        int cloudletIndex = random.nextInt(cloudletCount);
        int newVmId;
        do {
            newVmId = random.nextInt(vmCount);
        } while (newVmId == currentSolution[cloudletIndex] && vmCount > 1);
        
        neighborSolution[cloudletIndex] = newVmId;
        
        // Evaluate the neighbor solution
        double neighborFitness = calculateBeesFitness(neighborSolution);
        
        // Apply greedy selection
        if (neighborFitness > currentFitness) {
            // Accept better solution
            bee.setVmAllocation(neighborSolution);
            bee.setFitness(neighborFitness);
            bee.resetVisitCount();
        } else {
            // Keep current solution, increment visit count
            bee.incrementVisitCount();
        }
    }
    
    /**
     * Calculate probabilities for food sources based on their fitness values
     * 
     * @return Array of probabilities for each employed bee
     */
    private double[] calculateFoodSourceProbabilities() {
        // Find all employed bees (food sources)
        List<IndividualABC> employedBees = new ArrayList<>();
        for (IndividualABC bee : population.getBees()) {
            if (bee.getType() == IndividualABC.EMPLOYED) {
                employedBees.add(bee);
            }
        }
        
        // Calculate sum of fitnesses
        double totalFitness = 0;
        for (IndividualABC bee : employedBees) {
            totalFitness += bee.getFitness();
        }
        
        // Calculate probability for each food source
        double[] probabilities = new double[employedBees.size()];
        for (int i = 0; i < employedBees.size(); i++) {
            probabilities[i] = employedBees.get(i).getFitness() / totalFitness;
        }
        
        return probabilities;
    }
    
    /**
     * Onlooker Bee Phase
     * 
     * @param probabilities Probabilities for selecting food sources
     */
    private void onlookerBeePhase(double[] probabilities) {
        System.out.println("Onlooker Bee Phase");
        
        // Get list of employed bees
        List<IndividualABC> employedBees = new ArrayList<>();
        for (IndividualABC bee : population.getBees()) {
            if (bee.getType() == IndividualABC.EMPLOYED) {
                employedBees.add(bee);
            }
        }
        
        // Process each onlooker bee
        for (IndividualABC bee : population.getBees()) {
            if (bee.getType() == IndividualABC.ONLOOKER) {
                processOnlookerBee(bee, employedBees, probabilities);
            }
        }
    }
    
    /**
     * Process an onlooker bee - selects a food source, produces new food source, and applies greedy selection
     * 
     * @param bee The onlooker bee to process
     * @param employedBees List of employed bees
     * @param probabilities Probabilities for selecting food sources
     */
    private void processOnlookerBee(IndividualABC bee, List<IndividualABC> employedBees, double[] probabilities) {
        // Choose a food source based on probabilities
        IndividualABC selectedBee = selectFoodSource(employedBees, probabilities);
        
        // Generate neighbor solution from selected food source
        int[] selectedSolution = selectedBee.getVmAllocation();
        int[] neighborSolution = selectedSolution.clone();
        
        // Modify neighbor solution
        int cloudletIndex = random.nextInt(cloudletCount);
        int newVmId;
        do {
            newVmId = random.nextInt(vmCount);
        } while (newVmId == selectedSolution[cloudletIndex] && vmCount > 1);
        
        neighborSolution[cloudletIndex] = newVmId;
        
        // Calculate fitness for neighbor
        double neighborFitness = calculateBeesFitness(neighborSolution);
        
        // Apply greedy selection on the EMPLOYED bee (not the onlooker)
        double selectedFitness = selectedBee.getFitness();
        if (neighborFitness > selectedFitness) {
            selectedBee.setVmAllocation(neighborSolution);
            selectedBee.setFitness(neighborFitness);
            selectedBee.resetVisitCount();
        } else {
            selectedBee.incrementVisitCount();
        }
    }
    
    /**
     * Helper method to select a food source based on probabilities
     * 
     * @param employedBees List of employed bees
     * @param probabilities Array of probabilities
     * @return The selected employed bee
     */
    private IndividualABC selectFoodSource(List<IndividualABC> employedBees, double[] probabilities) {
        double r = random.nextDouble();
        double sum = 0;
        
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (r <= sum) {
                return employedBees.get(i);
            }
        }
        
        // Fallback to random selection if something goes wrong
        return employedBees.get(random.nextInt(employedBees.size()));
    }
    
    /**
     * Scout Bee Phase - check for abandoned sources and send scouts
     */
    private void scoutBeePhase() {
        System.out.println("Scout Bee Phase");
        
        for (IndividualABC bee : population.getBees()) {
            if (bee.getType() == IndividualABC.EMPLOYED && bee.getVisitCount() > limit) {
                System.out.println("Employed bee exceeded limit. Sending as scout.");
                sendScoutBee(bee);
            }
        }
    }
    
    /**
     * Send a scout bee to a random food source
     * 
     * @param bee The bee to send as scout
     */
    private void sendScoutBee(IndividualABC bee) {
        // Generate a completely new random solution
        int[] solution = bee.getVmAllocation();
        for (int j = 0; j < cloudletCount; j++) {
            solution[j] = random.nextInt(vmCount);
        }
        
        bee.resetVisitCount();
        bee.setFitness(calculateBeesFitness(solution));
    }
    
    /**
     * Update the global best solution if a better one is found
     */
    private void updateGlobalBest() {
        for (IndividualABC bee : population.getBees()) {
            if (globalBest == null || bee.getFitness() > globalBest.getFitness()) {
                if (globalBest == null) {
                    globalBest = new IndividualABC(-1, -1, cloudletCount);
                }
                globalBest.setVmAllocation(bee.getVmAllocation());
                globalBest.setFitness(bee.getFitness());
            }
        }
    }
    
    /**
     * Get the global best solution
     * 
     * @return The global best solution
     */
    public IndividualABC getGlobalBest() {
        return globalBest;
    }
    
    /**
     * Get the fitness of the global best solution
     * 
     * @return The global best fitness
     */
    public double getGlobalBestFitness() {
        return globalBest.getFitness();
    }
    
    /**
     * Get the makespan of the best solution
     * 
     * @return The makespan value
     */
    public double getBestMakespan() {
        return 1.0 / getGlobalBestFitness();
    }
} 