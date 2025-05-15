package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import java.util.List;

/**
 * Implementation of the standard Artificial Bee Colony (ABC) algorithm
 * following the classic ABC model with employed, onlooker, and scout bees.
 */
public class ABC {
    private PopulationABC population;
    private int maxIterations;
    private int currentIteration;
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    
    // ABC parameters
    private int numBees;         // Total swarm size
    private int numOnlookers;    // Number of onlooker bees (typically 50% of swarm)
    private int numEmployed;     // Number of employed bees (typically 50% of swarm)
    private int numScout;        // Number of scout bees (typically 1)
    private int limit;           // Limit of visits before abandonment
    private double probMistake;  // Kept for backward compatibility
    private double probPersuasion; // Kept for backward compatibility
    
    /**
     * Constructor for ABC algorithm
     * 
     * @param cloudletList List of cloudlets to be scheduled
     * @param vmList List of VMs available
     * @param numBees Total number of bees in the swarm
     * @param limit Maximum number of visits before abandoning a food source
     * @param maxIterations Maximum number of iterations
     */
    public ABC(List<Cloudlet> cloudletList, List<Vm> vmList, int numBees, 
               int limit, int maxIterations) {
        this.cloudletList = cloudletList;
        this.vmList = vmList;
        this.numBees = numBees;
        this.limit = limit;
        this.maxIterations = maxIterations;
        this.currentIteration = 0;
        
        // In standard ABC, the swarm is divided equally between employed and onlooker bees
        this.numEmployed = numBees / 2;
        this.numOnlookers = numBees / 2;
        this.numScout = 1;  // Standard ABC typically uses just 1 scout
        
        // Keep these parameters for backward compatibility
        this.probMistake = 0.1;
        this.probPersuasion = 0.5;
        
        // Initialize population
        this.population = new PopulationABC(numBees, numOnlookers, numEmployed, numScout, 
                                           limit, probMistake, probPersuasion,
                                           cloudletList.size(), vmList.size());
    }
    
    /**
     * Run the ABC algorithm
     * 
     * @return The optimized mapping of cloudlets to VMs
     */
    public int[] run() {
        // Initialize by calculating fitness for all bees
        population.calculateFitness(cloudletList, vmList);
        
        // Main loop - follows standard ABC algorithm steps
        for (currentIteration = 1; currentIteration <= maxIterations; currentIteration++) {
            System.out.println("ABC Iteration: " + currentIteration + "/" + maxIterations);
            population.iterate(cloudletList, vmList);
        }
        
        // Return the best solution found
        return population.getGlobalBest().getVmAllocation();
    }
    
    /**
     * Get the global best solution
     * 
     * @return The global best solution
     */
    public IndividualABC getGlobalBest() {
        return population.getGlobalBest();
    }
    
    /**
     * Get the global best fitness
     * 
     * @return The global best fitness
     */
    public double getGlobalBestFitness() {
        return population.getGlobalBest().getFitness();
    }
    
    /**
     * Get the makespan of the best solution
     * 
     * @return The makespan value
     */
    public double getBestMakespan() {
        // Since fitness is 1/makespan, we need to invert it
        return 1.0 / getGlobalBestFitness();
    }
} 