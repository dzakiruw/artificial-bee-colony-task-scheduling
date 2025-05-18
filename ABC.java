package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

public class ABC {
    // Parameters
    private int Imax; // Maximum number of iterations
    private int populationSize; // Population size (swarm size)
    private int employedBeeCount; // Number of employed bees (50% of population)
    private int onlookerBeeCount; // Number of onlooker bees (50% of population)
    private int scoutBeeCount; // Number of scout bees (exactly 1)
    private double limit; // Limit for scout bee phase
    private int[] abandonmentCounter; // Track abandoned food sources
    private double d; // EOBL coefficient
    private boolean useEOBL; // Flag to enable/disable EOBL

    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;

    private int numberOfDataCenters = 6;
    private double[] globalBestFitnesses;
    private int[][] globalBestPositions;

    public ABC(int Imax, int populationSize, double limit, double d, boolean useEOBL,
               List<Cloudlet> cloudletList, List<Vm> vmList, int chromosomeLength) {
        this.Imax = Imax;
        this.populationSize = populationSize;
        this.limit = limit;
        this.d = d;
        this.useEOBL = useEOBL;
        this.cloudletList = cloudletList;
        this.vmList = vmList;
        
        // Define bee counts according to requirements
        this.employedBeeCount = populationSize / 2;
        this.onlookerBeeCount = populationSize / 2;
        this.scoutBeeCount = 1;
        
        this.abandonmentCounter = new int[populationSize];
        
        globalBestFitnesses = new double[numberOfDataCenters];
        globalBestPositions = new int[numberOfDataCenters][];

        for (int i = 0; i < numberOfDataCenters; i++) {
            globalBestFitnesses[i] = Double.NEGATIVE_INFINITY;
            globalBestPositions[i] = null;
        }
    }

    // Initialize population (food sources)
    public Population initPopulation(int chromosomeLength, int dataCenterIterator) {
        Population population = new Population(this.populationSize, chromosomeLength, dataCenterIterator);
        return population;
    }

    // Evaluate fitness of food sources
    public void evaluateFitness(Population population, int dataCenterIterator, int cloudletIteration) {
        for (Individual individual : population.getIndividuals()) {
            double fitness = calcFitness(individual, dataCenterIterator, cloudletIteration);
            individual.setFitness(fitness);

            // Update global best if better solution found
            int dcIndex = dataCenterIterator - 1;
            if (fitness > globalBestFitnesses[dcIndex]) {
                globalBestFitnesses[dcIndex] = fitness;
                globalBestPositions[dcIndex] = individual.getChromosome().clone();
            }
        }
    }

    // Main ABC algorithm - following pseudocode exactly
    public void runABCAlgorithm(Population population, int dataCenterIterator, int cloudletIteration) {
        // Step 1: begin
        System.out.println("Starting ABC algorithm");
        
        // Step 2: Set iteration t = 1 (exact order from pseudocode)
        int iteration = 1;
        
        // Step 3: Define problem dimension (already done in initialization)
        System.out.println("Problem dimensions: " + population.getIndividual(0).getChromosomeLength());
        
        // Step 4: Generate initial population (already done externally)
        System.out.println("Initial population size: " + population.size());
        
        // Step 5: Evaluate the fitness of individuals
        evaluateFitness(population, dataCenterIterator, cloudletIteration);
        System.out.println("Initial fitness evaluation completed");
        
        // Reset abandonment counters
        for (int i = 0; i < abandonmentCounter.length; i++) {
            abandonmentCounter[i] = 0;
        }
        
        // Step 6: Main loop - termination condition check
        while (iteration <= Imax) {
            System.out.println("Starting iteration " + iteration + " of " + Imax);
            
            // Steps 7-10: Employed Bee Phase
            System.out.println("PHASE 1: Employed Bee Phase");
            employedBeePhase(population, dataCenterIterator, cloudletIteration);
            
            // Step 11: Calculate probability for each food source
            System.out.println("Calculating selection probabilities based on fitness");
            double[] probabilities = calculateProbabilities(population);
            
            // Steps 12-17: Onlooker Bee Phase
            System.out.println("PHASE 2: Onlooker Bee Phase");
            onlookerBeePhase(population, probabilities, dataCenterIterator, cloudletIteration);
            
            // Store Best Food Source Solution (explicit step from flowchart)
            System.out.println("Storing best food source found so far");
            storeBestFoodSource(population, dataCenterIterator);
            
            // Steps 18-21: Scout Bee Phase
            System.out.println("PHASE 3: Scout Bee Phase");
            // Step 19: Check if any employed bee becomes scout bee
            boolean scoutBeeExists = checkForScoutBees(population);
            
            // Step 20-21: If scout bee exists, send it to random food source
            if (scoutBeeExists) {
                System.out.println("Scout bee found in colony - performing scout bee step");
                scoutBeePhase(population, dataCenterIterator, cloudletIteration);
            } else {
                System.out.println("No food sources abandoned - no scout bee needed");
            }
            
            // Apply EOBL to improve solution quality (optional enhancement)
            if (useEOBL) {
                System.out.println("ENHANCEMENT: Applying EOBL to improve solution quality");
                applyEOBL(population, dataCenterIterator, cloudletIteration);
            }
            
            // Step 22: Increment iteration count
            iteration++;
            
            // Output current best
            int dcIndex = dataCenterIterator - 1;
            System.out.println("Iteration " + (iteration-1) + " Best Fitness for DC" + dataCenterIterator 
                    + ": " + globalBestFitnesses[dcIndex]);
            
            // Check termination condition (Step 6 continuation)
            if (iteration > Imax) {
                System.out.println("Termination criterion met - ending algorithm");
            } else {
                System.out.println("Termination criterion not met - continuing to next iteration");
            }
        }
        
        // Step 23-24: End of algorithm
        System.out.println("Final Best Solution Found - Fitness: " + 
                           globalBestFitnesses[dataCenterIterator - 1]);
    }
    
    // Check if any food source has reached abandonment limit (therefore, scout bee exists)
    private boolean checkForScoutBees(Population population) {
        for (int i = 0; i < employedBeeCount; i++) {
            if (abandonmentCounter[i] > limit) {
                return true; // Scout bee found in colony
            }
        }
        return false; // No scout bee in colony
    }
    
    // Store best food source - explicit step from flowchart
    private void storeBestFoodSource(Population population, int dataCenterIterator) {
        int dcIndex = dataCenterIterator - 1;
        
        // Find best solution in current population
        double bestFitness = Double.NEGATIVE_INFINITY;
        int bestIndex = -1;
        
        for (int i = 0; i < population.size(); i++) {
            Individual individual = population.getIndividual(i);
            if (individual.getFitness() > bestFitness) {
                bestFitness = individual.getFitness();
                bestIndex = i;
            }
        }
        
        // Update global best if needed
        if (bestFitness > globalBestFitnesses[dcIndex]) {
            globalBestFitnesses[dcIndex] = bestFitness;
            globalBestPositions[dcIndex] = population.getIndividual(bestIndex).getChromosome().clone();
            System.out.println("Updated Best Food Source with fitness: " + bestFitness);
        }
    }
    
    // Employed Bee Phase - steps 7-10 of pseudocode
    private void employedBeePhase(Population population, int dataCenterIterator, int cloudletIteration) {
        Random random = new Random();
        
        System.out.println("Employed Bee Phase: Processing " + employedBeeCount + " employed bees");
        System.out.println("Role: Collecting food from determined food sources for the hive");
        
        // Process only the employed bees (first half of population)
        for (int i = 0; i < employedBeeCount; i++) {
            Individual currentBee = population.getIndividual(i);
            
            // Step 8: Find a new food source
            // Create a new solution by modifying the current one
            int[] newSolution = currentBee.getChromosome().clone();
            
            // Select a random dimension to modify
            int dimension = random.nextInt(currentBee.getChromosomeLength());
            
            // Select another food source (excluding current one)
            int partnerIndex;
            do {
                partnerIndex = random.nextInt(employedBeeCount);
            } while (partnerIndex == i);
            
            Individual partner = population.getIndividual(partnerIndex);
            
            // Apply position bounds
            int minPosition = (dataCenterIterator - 1) * 9;
            int maxPosition = ((dataCenterIterator) * 9) - 1;
            
            // Calculate new position using ABC formula: vij = xij + φij(xij - xkj)
            int phi = random.nextInt(3) - 1; // Random value between -1, 0, 1
            int newValue = currentBee.getGene(dimension) + phi * (currentBee.getGene(dimension) - partner.getGene(dimension));
            
            // Ensure new position is within bounds
            if (newValue < minPosition) {
                newValue = minPosition;
            } else if (newValue > maxPosition) {
                newValue = maxPosition;
            }
            
            newSolution[dimension] = newValue;
            
            // Create new solution and evaluate
            Individual newBee = new Individual(newSolution);
            double newFitness = calcFitness(newBee, dataCenterIterator, cloudletIteration);
            newBee.setFitness(newFitness);
            
            // Step 9: Apply greedy selection mechanism
            // Greedy selection - if better, replace current solution
            if (newFitness > currentBee.getFitness()) {
                population.setIndividual(i, newBee);
                abandonmentCounter[i] = 0; // Reset abandonment counter
                
                // Update global best if needed
                int dcIndex = dataCenterIterator - 1;
                if (newFitness > globalBestFitnesses[dcIndex]) {
                    globalBestFitnesses[dcIndex] = newFitness;
                    globalBestPositions[dcIndex] = newBee.getChromosome().clone();
                }
            } else {
                // Increment abandonment counter for this food source
                abandonmentCounter[i]++;
            }
        }
    }
    
    // Calculate probabilities for onlooker bees based on fitness values - step 11
    private double[] calculateProbabilities(Population population) {
        double[] probabilities = new double[populationSize];
        double fitnessSum = 0;
        
        // Get sum of fitness values
        for (int i = 0; i < employedBeeCount; i++) {
            fitnessSum += population.getIndividual(i).getFitness();
        }
        
        // Calculate probability for each food source
        for (int i = 0; i < populationSize; i++) {
            if (i < employedBeeCount && fitnessSum > 0) {
                probabilities[i] = population.getIndividual(i).getFitness() / fitnessSum;
            } else {
                probabilities[i] = 0; // Non-employed bees get zero probability
            }
        }
        
        return probabilities;
    }
    
    // Onlooker Bee Phase - steps 12-17 of pseudocode
    private void onlookerBeePhase(Population population, double[] probabilities, int dataCenterIterator, int cloudletIteration) {
        Random random = new Random();
        
        System.out.println("Onlooker Bee Phase: Processing " + onlookerBeeCount + " onlooker bees");
        System.out.println("Role: Overseeing employed bees and verifying the quality of food sources");
        
        // Start index for onlooker bees
        int onlookerStartIndex = employedBeeCount;
        
        // Process onlooker bees
        int onlookerCount = 0;
        
        while (onlookerCount < onlookerBeeCount) {
            // Step 13: Choose a food source based on probability
            int selectedFoodSource = selectFoodSource(probabilities);
            
            // Step 14: Produce new food source
            Individual selectedBee = population.getIndividual(selectedFoodSource);
            int[] newSolution = selectedBee.getChromosome().clone();
            
            // Select a random dimension to modify
            int dimension = random.nextInt(selectedBee.getChromosomeLength());
            
            // Select another food source (excluding current one)
            int partnerIndex;
            do {
                partnerIndex = random.nextInt(employedBeeCount);
            } while (partnerIndex == selectedFoodSource);
            
            Individual partner = population.getIndividual(partnerIndex);
            
            // Apply position bounds
            int minPosition = (dataCenterIterator - 1) * 9;
            int maxPosition = ((dataCenterIterator) * 9) - 1;
            
            // Calculate new position using formula: vij = xij + φij(xij - xkj)
            int phi = random.nextInt(3) - 1; // Random value between -1, 0, 1
            int newValue = selectedBee.getGene(dimension) + 
                           phi * (selectedBee.getGene(dimension) - partner.getGene(dimension));
            
            // Ensure new position is within bounds
            if (newValue < minPosition) {
                newValue = minPosition;
            } else if (newValue > maxPosition) {
                newValue = maxPosition;
            }
            
            newSolution[dimension] = newValue;
            
            // Step 15: Evaluate the fitness
            Individual newBee = new Individual(newSolution);
            double newFitness = calcFitness(newBee, dataCenterIterator, cloudletIteration);
            newBee.setFitness(newFitness);
            
            // Save the new solution in the onlooker bee part of the population
            int onlookerIndex = onlookerStartIndex + onlookerCount;
            population.setIndividual(onlookerIndex, newBee);
            
            // Step 16: Apply greedy selection mechanism
            if (newFitness > selectedBee.getFitness()) {
                // If better, replace original food source - demonstrating "positive feedback"
                population.setIndividual(selectedFoodSource, newBee);
                abandonmentCounter[selectedFoodSource] = 0; // Reset abandonment counter
                
                // Update global best if needed
                int dcIndex = dataCenterIterator - 1;
                if (newFitness > globalBestFitnesses[dcIndex]) {
                    globalBestFitnesses[dcIndex] = newFitness;
                    globalBestPositions[dcIndex] = newBee.getChromosome().clone();
                }
            } else {
                // If not better, increment abandonment - demonstrating "negative feedback"
                abandonmentCounter[selectedFoodSource]++;
            }
            
            onlookerCount++;
        }
    }
    
    // Helper method to select a food source based on probability
    private int selectFoodSource(double[] probabilities) {
        Random random = new Random();
        double r = random.nextDouble();
        double sum = 0;
        
        for (int i = 0; i < employedBeeCount; i++) {
            sum += probabilities[i];
            if (r <= sum) {
                return i;
            }
        }
        
        // Fallback - return a random employed bee
        return random.nextInt(employedBeeCount);
    }
    
    // Scout Bee Phase - steps 18-21 of pseudocode
    private void scoutBeePhase(Population population, int dataCenterIterator, int cloudletIteration) {
        System.out.println("Scout Bee Phase: Looking for abandoned food sources");
        System.out.println("Role: Searching and exploring for new valid food source locations");
        
        // Find the most abandoned food source
        int maxAbandonmentIndex = -1;
        int maxAbandonmentCount = -1;
        
        for (int i = 0; i < employedBeeCount; i++) {
            if (abandonmentCounter[i] > maxAbandonmentCount) {
                maxAbandonmentCount = abandonmentCounter[i];
                maxAbandonmentIndex = i;
            }
        }
        
        // Step 20: If the most abandoned food source exceeds limit, send the scout bee to random food source
        if (maxAbandonmentCount > limit && maxAbandonmentIndex >= 0) {
            System.out.println("Food source at position " + maxAbandonmentIndex + 
                              " abandoned after " + maxAbandonmentCount + " trials (limit: " + limit + ")");
            System.out.println("Demonstrating 'Fluctuations' - Scout bee is exploring for new food source");
            
            // Generate completely new random solution (Scout bee exploring new area)
            Individual scout = new Individual(population.getIndividual(maxAbandonmentIndex).getChromosomeLength(), 
                                             dataCenterIterator);
            
            // Evaluate new solution
            double scoutFitness = calcFitness(scout, dataCenterIterator, cloudletIteration);
            scout.setFitness(scoutFitness);
            
            // Replace abandoned food source
            population.setIndividual(maxAbandonmentIndex, scout);
            abandonmentCounter[maxAbandonmentIndex] = 0;
            
            System.out.println("Scout bee found new food source with fitness: " + scoutFitness);
            
            // Update global best if needed - demonstrating "Multiple Interactions"
            int dcIndex = dataCenterIterator - 1;
            if (scoutFitness > globalBestFitnesses[dcIndex]) {
                globalBestFitnesses[dcIndex] = scoutFitness;
                globalBestPositions[dcIndex] = scout.getChromosome().clone();
                System.out.println("New food source is better than current best - information shared with colony");
            }
        } else {
            System.out.println("No food sources abandoned this iteration");
        }
    }

    // Enhanced Opposition-Based Learning (EOBL) - Optional enhancement to ABC algorithm
    // EOBL creates opposite solutions to the current best solution to potentially
    // find better solutions in unexplored regions of the search space
    public void applyEOBL(Population population, int dataCenterIterator, int cloudletIteration) {
        System.out.println("Applying Enhanced Opposition-Based Learning (EOBL)");
        System.out.println("EOBL enhances ABC by creating opposite solutions to explore unexplored regions");
        
        int dcIndex = dataCenterIterator - 1;

        // Define the search space boundaries
        int maxGene = ((dataCenterIterator) * 9) - 1;
        int minGene = (dataCenterIterator - 1) * 9;
        
        Random random = new Random();

        // Get current global best solution
        int[] currentGlobalBestPosition = globalBestPositions[dcIndex];
        double currentGlobalBestFitness = globalBestFitnesses[dcIndex];

        // Calculate the opposite solution
        int[] oppositeSolution = new int[currentGlobalBestPosition.length];
        int range = maxGene - minGene + 1;

        // Apply EOBL formula: opposite = (min + max)*d - original
        // Where d is the coefficient controlling the degree of opposition
        for (int i = 0; i < currentGlobalBestPosition.length; i++) {
            int candidate = (int) ((minGene + maxGene) * d) - currentGlobalBestPosition[i];

            // If the opposite value is outside boundaries, generate a random value
            if (candidate < minGene || candidate > maxGene) {
                candidate = minGene + random.nextInt(range);
            }

            oppositeSolution[i] = candidate;
        }
        
        // Create and evaluate the opposite individual
        Individual oppositeIndividual = new Individual(oppositeSolution);
        double oppositeFitness = calcFitness(oppositeIndividual, dataCenterIterator, cloudletIteration);
        oppositeIndividual.setFitness(oppositeFitness);

        // If the opposite solution is better, replace the current best
        if (oppositeFitness > currentGlobalBestFitness) {
            System.out.println("EOBL SUCCESS: New fitness = " + oppositeFitness + 
                              " (improved from " + currentGlobalBestFitness + ")");

            globalBestFitnesses[dcIndex] = oppositeFitness;
            globalBestPositions[dcIndex] = oppositeIndividual.getChromosome().clone();
        } else {
            System.out.println("EOBL did not find a better solution");
        }
    }

    // Calculate fitness based on the system fairness (Equation 1)
    public double calcFitness(Individual individual, int dataCenterIterator, int cloudletIteration) {
        double totalExecutionTime = 0;
        double totalCost = 0;
        int iterator = 0;
        dataCenterIterator = dataCenterIterator - 1;

        for (int i = 0 + dataCenterIterator * 9 + cloudletIteration * 54;
             i < 9 + dataCenterIterator * 9 + cloudletIteration * 54; i++) {
            int gene = individual.getGene(iterator);
            double mips = calculateMips(gene % 9);

            totalExecutionTime += cloudletList.get(i).getCloudletLength() / mips;
            totalCost += calculateCost(vmList.get(gene % 9), cloudletList.get(i));
            iterator++;
        }

        double makespanFitness = calculateMakespanFitness(totalExecutionTime);
        double costFitness = calculateCostFitness(totalCost);
        
        double fitness = makespanFitness + costFitness;

        return fitness;
    }

    private double calculateMips(int vmIndex) {
        double mips = 0;
        if (vmIndex % 9 == 0 || vmIndex % 9 == 3 || vmIndex % 9 == 6) {
            mips = 400;
        } else if (vmIndex % 9 == 1 || vmIndex % 9 == 4 || vmIndex % 9 == 7) {
            mips = 500;
        } else if (vmIndex % 9 == 2 || vmIndex % 9 == 5 || vmIndex % 9 == 8) {
            mips = 600;
        }
        return mips;
    }

    private double calculateCost(Vm vm, Cloudlet cloudlet) {
      double costPerMips = vm.getCostPerMips();
      double cloudletLength = cloudlet.getCloudletLength();
      double mips = vm.getMips();
      double executionTime = cloudletLength / mips;
      return costPerMips * executionTime;
    }

    private double calculateMakespanFitness(double totalExecutionTime) {
      // The higher the makespan, the lower the fitness
      return 1.0 / totalExecutionTime;
    }

    private double calculateCostFitness(double totalCost) {
      // The lower the cost, the higher the fitness
      return 1.0 / totalCost;
    }

    public int[] getBestVmAllocationForDatacenter(int dataCenterIterator) {
        return globalBestPositions[dataCenterIterator - 1];
    }

    public double getBestFitnessForDatacenter(int dataCenterIterator) {
        return globalBestFitnesses[dataCenterIterator - 1];
    }
}