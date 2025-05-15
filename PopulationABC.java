package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PopulationABC {
    private List<IndividualABC> bees;
    private int numBees;
    private int numInactive;
    private int numActive;
    private int numScout;
    private int maxVisits;
    private double probMistake;
    private double probPersuasion;
    private int cloudletCount;
    private int vmCount;
    private IndividualABC globalBest;
    
    /**
     * Constructor for ABC population
     * 
     * @param numBees Total number of bees
     * @param numInactive Number of inactive bees
     * @param numActive Number of active bees
     * @param numScout Number of scout bees
     * @param maxVisits Maximum visit count before abandoning a solution
     * @param probMistake Probability of making a mistake during solution update
     * @param probPersuasion Probability of persuasion for inactive bees
     * @param cloudletCount Number of cloudlets to schedule
     * @param vmCount Number of VMs available
     */
    public PopulationABC(int numBees, int numInactive, int numActive, int numScout, 
                        int maxVisits, double probMistake, double probPersuasion,
                        int cloudletCount, int vmCount) {
        this.numBees = numBees;
        this.numInactive = numInactive;
        this.numActive = numActive;
        this.numScout = numScout;
        this.maxVisits = maxVisits;
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
            if (i < numActive) {
                type = IndividualABC.ACTIVE;
            } else if (i < numActive + numInactive) {
                type = IndividualABC.INACTIVE;
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
     * Perform one iteration of the ABC algorithm
     * 
     * @param cloudletList List of cloudlets
     * @param vmList List of VMs
     * @return True if the global best solution was updated in this iteration
     */
    public boolean iterate(List<Cloudlet> cloudletList, List<Vm> vmList) {
        double previousBestFitness = globalBest != null ? globalBest.getFitness() : 0;
        
        // Process each bee based on its type
        for (IndividualABC bee : bees) {
            switch (bee.getType()) {
                case IndividualABC.ACTIVE:
                    processActiveBee(bee);
                    break;
                case IndividualABC.INACTIVE:
                    processInactiveBee(bee);
                    break;
                case IndividualABC.SCOUT:
                    processScoutBee(bee);
                    break;
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
     * Process an active bee - finds a neighbor solution and compares it
     * Following the pseudocode:
     * ProcessActiveBee()
     *  Generate a neighbor Solution.
     *  Generate a random number between zero to one in prob
     *  If (a neighbor solution quality > current bee solution quality)
     *   If (prob< Prob_Mistake)
     *    Increase numberOfVisits by one
     *   Else
     *    bee accepts the better neighbor solution
     *    Reset numberOfVisits to zero
     *    Update Current_Best_Solution
     *    Perform DoWaggleDance()
     *   End If
     *  End If
     *  If (a neighbor solution quality < current bee solution quality)
     *   If (prob< Prob_Mistake)
     *    bee accepts the better neighbor solution
     *    Reset numberOfVisits to zero
     *    Update Current_Best_Solution
     *    Perform DoWaggleDance()
     *   Else
     *    Increase numberOfVisits by one
     *   End If
     *   If (numberOfVisits> Max_number_of_Vists)
     *    Change the status of this active bee to be inactive
     *    Selected randomly inactive bee is to be active
     *  Return
     * 
     * @param bee The active bee to process
     */
    private void processActiveBee(IndividualABC bee) {
        // Generate a neighbor solution
        int[] currentSolution = bee.getVmAllocation();
        int[] neighborSolution = currentSolution.clone();
        
        Random random = new Random();
        int cloudletIndex = random.nextInt(cloudletCount);
        int newVmId;
        
        // Select a different VM for the cloudlet
        do {
            newVmId = random.nextInt(vmCount);
        } while (newVmId == currentSolution[cloudletIndex] && vmCount > 1);
        
        neighborSolution[cloudletIndex] = newVmId;
        
        // Store the current solution temporarily
        IndividualABC tempBee = new IndividualABC(-1, -1, cloudletCount);
        tempBee.setVmAllocation(neighborSolution);
        
        // Calculate fitness for the neighbor solution
        // Note: We don't have cloudletList and vmList here, so we'll assume
        // that the existing bee fitness is correct and compare solutions directly
        double currentFitness = bee.getFitness();
        
        // We'll compare a simple metric - the "balance" of VM assignments
        // Better distribution = higher fitness
        int[] vmCounts = new int[vmCount];
        for (int vmId : neighborSolution) {
            vmCounts[vmId]++;
        }
        
        // Calculate variance as a metric (lower variance = better distribution)
        double mean = (double) cloudletCount / vmCount;
        double variance = 0;
        for (int count : vmCounts) {
            variance += Math.pow(count - mean, 2);
        }
        variance /= vmCount;
        
        // Convert to fitness (higher is better)
        double neighborFitness = 1.0 / (1.0 + variance);
        
        // Generate a random number between zero and one
        double prob = random.nextDouble();
        
        // Compare fitness values (higher is better)
        if (neighborFitness > currentFitness) {
            // Neighbor solution is better
            if (prob < probMistake) {
                // Make a mistake and reject the better solution
                bee.incrementVisitCount();
            } else {
                // Accept the better solution
                bee.setVmAllocation(neighborSolution);
                bee.setFitness(neighborFitness);
                bee.resetVisitCount();
                // Update current best solution happens in calculateFitness
                
                // Perform waggle dance as per pseudocode
                doWaggleDance(bee);
            }
        } else {
            // Current solution is better
            if (prob < probMistake) {
                // Make a mistake and accept the worse solution
                bee.setVmAllocation(neighborSolution);
                bee.setFitness(neighborFitness);
                bee.resetVisitCount();
                
                // Perform waggle dance even with the worse solution
                doWaggleDance(bee);
            } else {
                // Correctly reject the worse solution
                bee.incrementVisitCount();
            }
        }
        
        // Check if the bee should abandon the solution
        if (bee.getVisitCount() > maxVisits) {
            // Change this active bee to be inactive according to the pseudocode
            // 1. Find a random inactive bee
            List<IndividualABC> inactiveBees = new ArrayList<>();
            for (IndividualABC currentBee : bees) {
                if (currentBee.getType() == IndividualABC.INACTIVE) {
                    inactiveBees.add(currentBee);
                }
            }
            
            if (!inactiveBees.isEmpty()) {
                // Select a random inactive bee
                IndividualABC inactiveBee = inactiveBees.get(random.nextInt(inactiveBees.size()));
                
                // Change statuses
                bee.setType(IndividualABC.INACTIVE);  // Active becomes inactive
                inactiveBee.setType(IndividualABC.ACTIVE);  // Inactive becomes active
                
                // Reset visit counts
                bee.resetVisitCount();
                inactiveBee.resetVisitCount();
                
                // Generate new solution for the formerly active bee
                bee.generateRandomSolution(vmCount);
                
                System.out.println("Changed bee " + bee.getId() + " from ACTIVE to INACTIVE");
                System.out.println("Changed bee " + inactiveBee.getId() + " from INACTIVE to ACTIVE");
            } else {
                // If no inactive bees exist, just reset this bee (fallback)
                bee.resetVisitCount();
                bee.generateRandomSolution(vmCount);
                System.out.println("No inactive bees available, reset active bee " + bee.getId());
            }
        }
    }
    
    /**
     * Process an inactive bee - may follow active bees
     * 
     * @param bee The inactive bee to process
     */
    private void processInactiveBee(IndividualABC bee) {
        Random random = new Random();
        
        // Generate a random number for persuasion check
        double prob = random.nextDouble();
        
        if (prob < probPersuasion && globalBest != null) {
            // Copy the global best solution with small modification
            int[] bestSolution = globalBest.getVmAllocation();
            int[] newSolution = bestSolution.clone();
            
            // Make small change to one cloudlet assignment
            int cloudletIndex = random.nextInt(cloudletCount);
            int newVmId;
            do {
                newVmId = random.nextInt(vmCount);
            } while (newVmId == bestSolution[cloudletIndex] && vmCount > 1);
            
            newSolution[cloudletIndex] = newVmId;
            bee.setVmAllocation(newSolution);
            
            // The fitness will be recalculated in the next iteration
        } else {
            // Generate a completely new solution
            bee.generateRandomSolution(vmCount);
        }
    }
    
    /**
     * Process a scout bee - always explores new solutions
     * 
     * @param bee The scout bee to process
     */
    private void processScoutBee(IndividualABC bee) {
        // Store the current solution and fitness
        double currentFitness = bee.getFitness();
        
        // Generate a random solution
        int[] randomSolution = new int[cloudletCount];
        Random random = new Random();
        for (int i = 0; i < cloudletCount; i++) {
            randomSolution[i] = random.nextInt(vmCount);
        }
        
        // Calculate fitness for the random solution using a simple metric
        // (same approach as in processActiveBee)
        int[] vmCounts = new int[vmCount];
        for (int vmId : randomSolution) {
            vmCounts[vmId]++;
        }
        
        // Calculate variance as a metric (lower variance = better distribution)
        double mean = (double) cloudletCount / vmCount;
        double variance = 0;
        for (int count : vmCounts) {
            variance += Math.pow(count - mean, 2);
        }
        variance /= vmCount;
        
        // Convert to fitness (higher is better)
        double randomFitness = 1.0 / (1.0 + variance);
        
        // Compare fitness values (higher is better)
        if (randomFitness > currentFitness) {
            // Scout bee accepts the random solution
            bee.setVmAllocation(randomSolution);
            bee.setFitness(randomFitness);
            
            // Update Current_Best_Solution happens in calculateFitness
            
            // Perform waggle dance
            doWaggleDance(bee);
        }
        
        // The actual fitness will be recalculated in the next iteration
    }
    
    /**
     * Implementation of DoWaggleDance - share information with inactive bees
     * Following the pseudocode:
     * DoWaggleDance ()
     *  For i :=1 to Number_of_Inactive
     *   If (Dancing bee solution quality > current inactive bee solution quality)
     *    Generate a random number between zero to one in prob2
     *    If (Prob_Presuasion> prob2)
     *     Current inactive bee accepts the solution of Dancing bee
     *    End If
     *   End If
     * Return
     * 
     * @param dancingBee The bee performing the waggle dance
     */
    private void doWaggleDance(IndividualABC dancingBee) {
        // Get the dancing bee's solution and fitness
        int[] dancingSolution = dancingBee.getVmAllocation();
        double dancingFitness = dancingBee.getFitness();
        
        // Create a temporary storage for the dancing bee solution
        // with a small modification to ensure uniqueness
        int[] modifiedDancingSolution = dancingSolution.clone();
        Random random = new Random();
        
        // Count of inactive bees processed
        int inactiveCount = 0;
        
        // Iterate through all bees in the population
        for (IndividualABC bee : bees) {
            // Check if this is an inactive bee
            if (bee.getType() == IndividualABC.INACTIVE) {
                inactiveCount++;
                
                // If we've processed all inactive bees, stop
                if (inactiveCount > numInactive) {
                    break;
                }
                
                // Check if dancing bee's solution is better than current inactive bee's solution
                if (dancingFitness > bee.getFitness()) {
                    // Generate a random number for persuasion check
                    double prob2 = random.nextDouble();
                    
                    // Check if inactive bee accepts the solution
                    if (probPersuasion > prob2) {
                        // Create a slightly modified version of the dancing bee's solution
                        // to ensure diversity in the population
                        if (cloudletCount > 0) {
                            int modificationIndex = random.nextInt(cloudletCount);
                            int newVmId;
                            do {
                                newVmId = random.nextInt(vmCount);
                            } while (newVmId == modifiedDancingSolution[modificationIndex] && vmCount > 1);
                            
                            modifiedDancingSolution[modificationIndex] = newVmId;
                        }
                        
                        // Current inactive bee accepts the solution
                        bee.setVmAllocation(modifiedDancingSolution);
                        
                        // The fitness will be recalculated in the next iteration
                    }
                }
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
     * Get all bees in the population
     * 
     * @return List of all bees
     */
    public List<IndividualABC> getAllBees() {
        return bees;
    }
}
