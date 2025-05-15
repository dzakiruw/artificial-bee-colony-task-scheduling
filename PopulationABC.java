package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PopulationABC {
    private List<IndividualABC> bees;
    private int numBees;
    private int numOnlooker;
    private int numEmployed;
    private int numScout;
    private int limit;  // Abandonment limit (formerly maxVisits)
    private double probMistake;  // Kept for backward compatibility
    private double probPersuasion;  // Kept for backward compatibility
    private int cloudletCount;
    private int vmCount;
    private IndividualABC globalBest;
    
    /**
     * Constructor for ABC population
     * 
     * @param numBees Total number of bees
     * @param numOnlooker Number of onlooker bees
     * @param numEmployed Number of employed bees
     * @param numScout Number of scout bees (typically 1)
     * @param limit Limit of visits before abandoning a food source
     * @param probMistake Probability of making a mistake (kept for compatibility)
     * @param probPersuasion Probability of persuasion (kept for compatibility)
     * @param cloudletCount Number of cloudlets to schedule
     * @param vmCount Number of VMs available
     */
    public PopulationABC(int numBees, int numOnlooker, int numEmployed, int numScout, 
                        int limit, double probMistake, double probPersuasion,
                        int cloudletCount, int vmCount) {
        this.numBees = numBees;
        this.numOnlooker = numOnlooker;
        this.numEmployed = numEmployed;
        this.numScout = numScout;
        this.limit = limit;
        this.probMistake = probMistake;
        this.probPersuasion = probPersuasion;
        this.cloudletCount = cloudletCount;
        this.vmCount = vmCount;
        this.bees = new ArrayList<>();
        this.globalBest = null;
        
        initializePopulation();
    }

    /**
     * Initialize the bee population with random solutions
     */
    private void initializePopulation() {
        for (int i = 0; i < numBees; i++) {
            int type;
            if (i < numEmployed) {
                type = IndividualABC.EMPLOYED;
            } else if (i < numEmployed + numOnlooker) {
                type = IndividualABC.ONLOOKER;
            } else {
                type = IndividualABC.SCOUT;
            }
            
            IndividualABC bee = new IndividualABC(i, type, cloudletCount);
            bee.generateRandomSolution(vmCount);
            bees.add(bee);
        }
    }
    
    /**
     * Calculate fitness for all bees in the population
     * 
     * @param cloudletList List of cloudlets
     * @param vmList List of VMs
     */
    public void calculateFitness(List<Cloudlet> cloudletList, List<Vm> vmList) {
        for (IndividualABC bee : bees) {
            double fitness = calculateBeesFitness(bee, cloudletList, vmList);
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
     * Calculate fitness for a specific bee
     * 
     * @param bee The bee to calculate fitness for
     * @param cloudletList List of cloudlets
     * @param vmList List of VMs
     * @return The fitness value
     */
    private double calculateBeesFitness(IndividualABC bee, List<Cloudlet> cloudletList, List<Vm> vmList) {
        int[] vmAllocation = bee.getVmAllocation();
        
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
     * Perform one iteration of the ABC algorithm following the standard pseudocode
     * 
     * @param cloudletList List of cloudlets
     * @param vmList List of VMs
     * @return True if the global best solution was updated in this iteration
     */
    public boolean iterate(List<Cloudlet> cloudletList, List<Vm> vmList) {
        double previousBestFitness = globalBest != null ? globalBest.getFitness() : 0;
        
        // PHASE 1: Employed Bee Phase
        System.out.println("Phase 1: Employed Bee Phase");
        for (IndividualABC bee : bees) {
            if (bee.getType() == IndividualABC.EMPLOYED) {
                processEmployedBee(bee, cloudletList, vmList);
            }
        }
        
        // Calculate probabilities for each food source
        double[] probabilities = calculateFoodSourceProbabilities();
        
        // PHASE 2: Onlooker Bee Phase
        System.out.println("Phase 2: Onlooker Bee Phase");
        for (IndividualABC bee : bees) {
            if (bee.getType() == IndividualABC.ONLOOKER) {
                processOnlookerBee(bee, probabilities, cloudletList, vmList);
            }
        }
        
        // PHASE 3: Scout Bee Phase
        System.out.println("Phase 3: Scout Bee Phase");
        for (IndividualABC bee : bees) {
            if (bee.getType() == IndividualABC.EMPLOYED && bee.getVisitCount() > limit) {
                // Convert employed bee to scout temporarily
                System.out.println("Employed bee " + bee.getId() + " exceeded limit. Sending as scout.");
                scoutBeePhase(bee);
            }
        }
        
        // Recalculate fitness for all bees with the actual CloudSim models
        calculateFitness(cloudletList, vmList);
        
        // Find and update the global best solution if improved
        for (IndividualABC bee : bees) {
            if (globalBest == null || bee.getFitness() > globalBest.getFitness()) {
                if (globalBest == null) {
                    globalBest = new IndividualABC(-1, -1, cloudletCount);
                }
                globalBest.setVmAllocation(bee.getVmAllocation());
                globalBest.setFitness(bee.getFitness());
            }
        }
        
        // Return true if global best was updated
        return globalBest != null && globalBest.getFitness() > previousBestFitness;
    }
    
    /**
     * Calculate probabilities for food sources based on their fitness values
     * 
     * @return Array of probabilities for each employed bee
     */
    private double[] calculateFoodSourceProbabilities() {
        // Find all employed bees (food sources)
        List<IndividualABC> employedBees = new ArrayList<>();
        for (IndividualABC bee : bees) {
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
     * Process an employed bee - finds a neighbor solution and applies greedy selection
     * 
     * @param bee The employed bee to process
     * @param cloudletList List of cloudlets
     * @param vmList List of VMs
     */
    private void processEmployedBee(IndividualABC bee, List<Cloudlet> cloudletList, List<Vm> vmList) {
        // Store original solution and fitness
        int[] currentSolution = bee.getVmAllocation();
        double currentFitness = bee.getFitness();
        
        // Generate a neighbor solution
        int[] neighborSolution = currentSolution.clone();
        Random random = new Random();
        
        // Modify the solution by changing one cloudlet's VM assignment
        int cloudletIndex = random.nextInt(cloudletCount);
        int newVmId;
        do {
            newVmId = random.nextInt(vmCount);
        } while (newVmId == currentSolution[cloudletIndex] && vmCount > 1);
        
        neighborSolution[cloudletIndex] = newVmId;
        
        // Evaluate the neighbor solution
        IndividualABC neighborBee = new IndividualABC(-1, -1, cloudletCount);
        neighborBee.setVmAllocation(neighborSolution);
        double neighborFitness = calculateBeesFitness(neighborBee, cloudletList, vmList);
        
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
     * Process an onlooker bee - selects a food source, produces new food source, and applies greedy selection
     * 
     * @param bee The onlooker bee to process
     * @param probabilities Probabilities for selecting food sources
     * @param cloudletList List of cloudlets
     * @param vmList List of VMs
     */
    private void processOnlookerBee(IndividualABC bee, double[] probabilities, 
                                List<Cloudlet> cloudletList, List<Vm> vmList) {
        // Get list of employed bees
        List<IndividualABC> employedBees = new ArrayList<>();
        for (IndividualABC currentBee : bees) {
            if (currentBee.getType() == IndividualABC.EMPLOYED) {
                employedBees.add(currentBee);
            }
        }
        
        // Choose a food source based on probabilities
        IndividualABC selectedBee = selectFoodSource(employedBees, probabilities);
        
        // Generate neighbor solution from selected food source
        int[] selectedSolution = selectedBee.getVmAllocation();
        int[] neighborSolution = selectedSolution.clone();
        
        // Modify neighbor solution
        Random random = new Random();
        int cloudletIndex = random.nextInt(cloudletCount);
        int newVmId;
        do {
            newVmId = random.nextInt(vmCount);
        } while (newVmId == selectedSolution[cloudletIndex] && vmCount > 1);
        
        neighborSolution[cloudletIndex] = newVmId;
        
        // Calculate fitness for neighbor
        IndividualABC neighborBee = new IndividualABC(-1, -1, cloudletCount);
        neighborBee.setVmAllocation(neighborSolution);
        double neighborFitness = calculateBeesFitness(neighborBee, cloudletList, vmList);
        
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
        Random random = new Random();
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
     * Scout bee phase - generates a random solution for the abandoned employed bee
     * 
     * @param bee The bee to process
     */
    private void scoutBeePhase(IndividualABC bee) {
        // Generate a completely new random solution
        bee.generateRandomSolution(vmCount);
        bee.resetVisitCount();
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
     * Get all bees in the population
     * 
     * @return List of all bees
     */
    public List<IndividualABC> getAllBees() {
        return bees;
    }
}
