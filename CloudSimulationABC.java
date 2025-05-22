package org.cloudbus.cloudsim.examples;

import java.util.Locale;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.DoubleStream;

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


/**
 * Kelas CloudSimulationABC
 * 
 * Kelas ini mengimplementasikan simulasi penjadwalan tugas di lingkungan komputasi awan
 * menggunakan algoritma Artificial Bee Colony (ABC) dengan atau tanpa peningkatan EOABC.
 * Kelas ini mensimulasikan beberapa pusat data dengan sejumlah host dan mesin virtual (VM)
 * untuk memproses beban kerja (cloudlet) dari berbagai dataset.
 * 
 * Tujuan utama adalah optimasi penjadwalan tugas untuk meminimalkan makespan dan biaya.
 */
public class CloudSimulationABC {
    // Variabel untuk pusat data yang akan digunakan dalam simulasi
    private static PowerDatacenter datacenter1, datacenter2, datacenter3, datacenter4, datacenter5, datacenter6;
    
    // Daftar untuk menyimpan cloudlet (tugas) dan mesin virtual
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;
    
    // Penulis file CSV untuk menyimpan hasil simulasi
    private static BufferedWriter csvWriter;

    // Parameter konfigurasi simulasi
    // Apakah menggunakan peningkatan Elite Opposition-Based Artificial Bee Colony
    private static final boolean USE_EOABC = false; 
    
    // Jumlah maksimum iterasi untuk algoritma ABC
    private static final int MAX_ITERATIONS = 5;   
    
    // Ukuran populasi lebah - harus genap untuk mendukung proporsi 50/50
    private static final int POPULATION_SIZE = 40; 
    
    // Koefisien EOABC yang mengontrol tingkat oposisi
    private static final double EOABC_COEFFICIENT = 0.3; 
    
    // Jumlah percobaan yang akan dijalankan
    private static final int NUM_TRIALS = 10;      
    
    // Konfigurasi dataset
    // Jenis dataset yang digunakan: "RandSimple", "RandStratified", atau "SDSC"
    private static String DATASET_TYPE = "SDSC"; 
    
    // Ukuran dataset untuk RandSimple dan RandStratified (dikalikan 1000)
    private static int DATASET_SIZE = 10; 
    // Dataset SDSC memiliki ukuran tetap 7395

    /**
     * Metode utama yang menjalankan simulasi
     * 
     * Metode ini mengatur parameter simulasi, menangani argumen baris perintah,
     * menjalankan beberapa uji coba simulasi, dan menulis hasil ke file CSV.
     */
    public static void main(String[] args) {
        // Mengatur locale ke English-US untuk format angka yang konsisten
        Locale.setDefault(new Locale("en", "US"));
        
        // Memproses argumen baris perintah jika disediakan
        if (args.length >= 1) {
            String datasetArg = args[0].trim();
            if (datasetArg.equals("RandSimple") || datasetArg.equals("RandStratified") || datasetArg.equals("SDSC")) {
                DATASET_TYPE = datasetArg;
                Log.printLine("Dataset type set via command line: " + DATASET_TYPE);
            }
            
            if (args.length >= 2 && !DATASET_TYPE.equals("SDSC")) {
                try {
                    int size = Integer.parseInt(args[1]);
                    if (size > 0) {
                        DATASET_SIZE = size;
                        Log.printLine("Dataset size set via command line: " + DATASET_SIZE);
                    }
                } catch (NumberFormatException e) {
                    Log.printLine("Invalid dataset size argument. Using default: " + DATASET_SIZE);
                }
            }
        }
        
        Log.printLine("Starting Cloud Simulation with ABC" + (USE_EOABC ? "+EOABC" : "") + " using " + DATASET_TYPE + " dataset...");

        try {
            // Menyiapkan file CSV untuk hasil simulasi
            String resultFileName = "abc_" + DATASET_TYPE + "_" + (DATASET_TYPE.equals("SDSC") ? "7395" : DATASET_SIZE + "000") + "_results.csv";
            csvWriter = new BufferedWriter(new FileWriter(resultFileName));
            // Menulis header CSV
            csvWriter.write("Trial,Total Wait Time,Average Start Time,Average Execution Time,Average Finish Time,Throughput,Makespan,Imbalance Degree,Total Scheduling Length,Resource Utilization,Energy Consumption\n");
            
            // Menjalankan beberapa percobaan simulasi
            for (int trial = 1; trial <= NUM_TRIALS; trial++) {
                Log.printLine("\n\n========== TRIAL " + trial + " OF " + NUM_TRIALS + " ==========\n");
                runSimulation(trial);
            }
            
            // Menutup file CSV setelah semua percobaan selesai
            csvWriter.close();
            Log.printLine("\nAll trials completed. Results written to " + resultFileName);
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Simulation terminated due to an error");
            try {
                if (csvWriter != null) {
                    csvWriter.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
    
    /**
     * Metode untuk menjalankan satu simulasi
     * 
     * Metode ini menginisialisasi CloudSim, membuat pusat data, broker, VM, dan cloudlet.
     * Kemudian menjalankan algoritma ABC untuk setiap pusat data dan iterasi cloudlet,
     * dan akhirnya mencetak hasil.
     * 
     * @param trialNum Nomor percobaan saat ini
     */
    private static void runSimulation(int trialNum) throws Exception {
        // Jumlah pengguna dalam simulasi
        int num_user = 1;
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false;

        // Inisialisasi CloudSim
        CloudSim.init(num_user, calendar, trace_flag);

        // Inisialisasi variabel hostId untuk pembuatan pusat data
        int hostId = 0;

        // Membuat enam pusat data dengan karakteristik yang sama
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

        // Membuat broker data center yang mengelola VM dan cloudlet
        DatacenterBroker broker = createBroker();
        int brokerId = broker.getId();
        int vmNumber = 54; // Jumlah total mesin virtual
        
        // Menentukan jumlah cloudlet berdasarkan jenis dataset
        int cloudletNumber;
        if (DATASET_TYPE.equals("SDSC")) {
            cloudletNumber = 7395; // SDSC memiliki ukuran tetap
        } else {
            // Untuk RandSimple dan RandStratified
            cloudletNumber = DATASET_SIZE * 1000;
        }
        
        Log.printLine("Running simulation with " + DATASET_TYPE + " dataset, " + cloudletNumber + " cloudlets");

        // Membuat VM dan cloudlet
        vmlist = createVM(brokerId, vmNumber);
        cloudletList = createCloudlet(brokerId, cloudletNumber);

        // Mengirimkan VM dan cloudlet ke broker
        broker.submitVmList(vmlist);
        broker.submitCloudletList(cloudletList);

        // Menghitung jumlah iterasi cloudlet berdasarkan jumlah VM
        int cloudletLoopingNumber = cloudletNumber / vmNumber - 1;

        // Iterasi untuk setiap batch cloudlet
        for (int cloudletIterator = 0; cloudletIterator <= cloudletLoopingNumber; cloudletIterator++) {
            System.out.println("Cloudlet Iteration Number " + cloudletIterator);

            // Iterasi untuk setiap pusat data
            for (int dataCenterIterator = 1; dataCenterIterator <= 6; dataCenterIterator++) {
                
                // Parameter untuk algoritma ABC
                int Imax = MAX_ITERATIONS; // Iterasi maksimum
                int populationSize = POPULATION_SIZE; // Ukuran populasi lebah
                
                // Dalam algoritma ABC, "dimensi" mengacu pada jumlah variabel keputusan dalam solusi
                // Dalam kasus kita, setiap pusat data memproses 9 cloudlet sekaligus
                int dimensions = 9; // Setiap pusat data memproses 9 cloudlet
                
                // Parameter "limit" menentukan kapan sumber makanan harus ditinggalkan
                // Dalam literatur ABC, ini biasanya dihitung sebagai:
                // limit = 0.5 * (jumlah lebah pekerja * dimensi)
                double limit = 0.4 * (populationSize/2) * dimensions;
                
                // Koefisien EOABC (peningkatan opsional)
                double d = EOABC_COEFFICIENT;
                
                // Menampilkan konfigurasi algoritma ABC
                System.out.println("\n====== ABC ALGORITHM CONFIGURATION ======");
                System.out.println("- Swarm Size: " + populationSize);
                System.out.println("- Employed Bees: " + populationSize/2 + " (50% of swarm)");
                System.out.println("- Onlooker Bees: " + populationSize/2 + " (50% of swarm)");
                System.out.println("- Scout Bees: 1");
                System.out.println("- Dimensions: " + dimensions);
                System.out.println("- Limit: " + limit);
                System.out.println("- Max Iterations: " + Imax);
                System.out.println("- EOABC Enhancement: " + (USE_EOABC ? "Enabled (d=" + d + ")" : "Disabled"));
                System.out.println("========================================\n");
                
                // Inisialisasi algoritma ABC
                ABC abc = new ABC(Imax, populationSize, limit, d, USE_EOABC, cloudletList, vmlist, cloudletNumber);

                // Inisialisasi populasi
                System.out.println("Datacenter " + dataCenterIterator + " Population Initialization");
                Population population = abc.initPopulation(cloudletNumber, dataCenterIterator);

                // Menjalankan algoritma ABC
                abc.runABCAlgorithm(population, dataCenterIterator, cloudletIterator);

                // Mendapatkan solusi terbaik
                int[] bestSolution = abc.getBestVmAllocationForDatacenter(dataCenterIterator);
                double bestFitness = abc.getBestFitnessForDatacenter(dataCenterIterator);
                
                System.out.println("Best solution found for datacenter " + dataCenterIterator + 
                                  " with fitness " + bestFitness);

                // Menetapkan tugas ke VM berdasarkan solusi terbaik
                for (int assigner = 0 + (dataCenterIterator - 1) * 9 + cloudletIterator * 54;
                     assigner < 9 + (dataCenterIterator - 1) * 9 + cloudletIterator * 54; assigner++) {
                    int vmId = bestSolution[assigner - (dataCenterIterator - 1) * 9 - cloudletIterator * 54];
                    broker.bindCloudletToVm(assigner, vmId);
                }
                

            }
        }

        // Memulai simulasi dan mencetak hasil
        CloudSim.startSimulation();

        // Mendapatkan daftar cloudlet yang telah diterima
        List<Cloudlet> newList = broker.getCloudletReceivedList();

        // Menghentikan simulasi
        CloudSim.stopSimulation();

        // Mencetak daftar cloudlet dan statistik
        printCloudletList(newList, trialNum);

        Log.printLine("Cloud Simulation with ABC" + (USE_EOABC ? "+EOABC" : "") + 
                     " Trial " + trialNum + " using " + DATASET_TYPE + " dataset finished!");
    }

    /**
     * Membuat daftar cloudlet
     * 
     * Metode ini membaca data panjang tugas dari file dataset dan membuat cloudlet
     * dengan karakteristik yang sesuai.
     * 
     * @param userId ID pengguna yang akan memiliki cloudlet
     * @param cloudlets Jumlah cloudlet yang akan dibuat
     * @return Daftar cloudlet yang dibuat
     */
    private static List<Cloudlet> createCloudlet(int userId, int cloudlets) {
        // Mendapatkan nilai panjang tugas dari file dataset
        ArrayList<Double> randomSeed = getSeedValue(cloudlets);

        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

        // Parameter cloudlet
        long fileSize = 300; // Ukuran file input (byte)
        long outputSize = 300; // Ukuran file output (byte)
        int pesNumber = 1; // Jumlah CPU core yang dibutuhkan
        UtilizationModel utilizationModel = new UtilizationModelFull();

        // Membuat cloudlet dengan panjang tugas yang berbeda
        for (int i = 0; i < cloudlets; i++) {
            long length = 0;

            if (randomSeed.size() > i) {
                length = Double.valueOf(randomSeed.get(i)).longValue();
            }

            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(userId);
            list.add(cloudlet);
        }
        // Mengacak urutan cloudlet untuk simulasi yang lebih realistis
        Collections.shuffle(list);

        return list;
    }

    /**
     * Membuat daftar VM 
     * 
     * Metode ini membuat sejumlah VM dengan karakteristik yang berbeda.
     * VM dibuat dengan tiga jenis konfigurasi yang berbeda (RAM, MIPS).
     * 
     * @param userId ID pengguna yang akan memiliki VM
     * @param vms Jumlah VM yang akan dibuat
     * @return Daftar VM yang dibuat
     */
    private static List<Vm> createVM(int userId, int vms) {
        LinkedList<Vm> list = new LinkedList<Vm>();

        // Parameter VM
        long size = 10000; // Ukuran image (MB)
        int[] ram = { 512, 1024, 2048 }; // RAM (MB) untuk setiap jenis VM
        int[] mips = { 400, 500, 600 }; // Kecepatan processor (MIPS) untuk setiap jenis VM
        long bw = 1000; // Bandwidth (Mb/s)
        int pesNumber = 1; // Jumlah CPU core
        String vmm = "Xen"; // Virtual Machine Monitor

        Vm[] vm = new Vm[vms];

        // Membuat VM dengan karakteristik yang berbeda-beda
        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(i, userId, mips[i % 3], pesNumber, ram[i % 3], bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }

        return list;
    }

    /**
     * Mendapatkan nilai panjang tugas dari file dataset
     * 
     * Metode ini membaca nilai panjang tugas dari file dataset yang dipilih.
     * 
     * @param cloudletcount Jumlah cloudlet yang dibutuhkan
     * @return Daftar nilai panjang tugas
     */
    private static ArrayList<Double> getSeedValue(int cloudletcount) {
        ArrayList<Double> seed = new ArrayList<Double>();
        try {
            File fobj;
            
            // Memilih file dataset yang benar berdasarkan jenis dataset
            if (DATASET_TYPE.equals("SDSC")) {
                fobj = new File(System.getProperty("user.dir") + "/cloudsim-3.0.3/datasets/SDSC/SDSC7395.txt");
            } else if (DATASET_TYPE.equals("RandSimple")) {
                fobj = new File(System.getProperty("user.dir") + "/cloudsim-3.0.3/datasets/randomSimple/RandSimple"+DATASET_SIZE+"000.txt");
            } else if (DATASET_TYPE.equals("RandStratified")) {
                fobj = new File(System.getProperty("user.dir") + "/cloudsim-3.0.3/datasets/randomStratified/RandStratified"+DATASET_SIZE+"000.txt");
            } else {
                Log.printLine("Invalid dataset type: " + DATASET_TYPE + ". Defaulting to RandSimple.");
                fobj = new File(System.getProperty("user.dir") + "/cloudsim-3.0.3/datasets/randomSimple/RandSimple"+DATASET_SIZE+"000.txt");
            }
            
            Log.printLine("Loading dataset file: " + fobj.getPath());
            java.util.Scanner readFile = new java.util.Scanner(fobj);

            // Membaca nilai dari file dataset
            while (readFile.hasNextLine() && cloudletcount > 0) {
                seed.add(readFile.nextDouble());
                cloudletcount--;
            }
            readFile.close();

        } catch (FileNotFoundException e) {
            Log.printLine("ERROR: Dataset file not found. Please check the path and file name.");
            e.printStackTrace();
        }

        return seed;
    }

    /**
     * Membuat pusat data dengan karakteristik tertentu
     * 
     * Metode ini membuat pusat data dengan karakteristik yang ditentukan
     * termasuk daftar host, kebijakan alokasi VM, dan model daya.
     * 
     * @param name Nama pusat data
     * @param hostId ID awal untuk host di pusat data
     * @return PowerDatacenter Pusat data yang dibuat
     */
    private static PowerDatacenter createDatacenter(String name, int hostId) {
        // Daftar host di pusat data
        List<PowerHost> hostList = new ArrayList<PowerHost>();

        // Daftar Processing Elements (PE) untuk setiap host
        List<Pe> peList1 = new ArrayList<Pe>();
        List<Pe> peList2 = new ArrayList<Pe>();
        List<Pe> peList3 = new ArrayList<Pe>();

        // Kecepatan PE untuk setiap host
        int mipsunused = 300; // MIPS yang tidak digunakan
        int mips1 = 400; // MIPS untuk host 1
        int mips2 = 500; // MIPS untuk host 2
        int mips3 = 600; // MIPS untuk host 3

        // Menambahkan PE ke daftar PE untuk setiap host
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

        // Parameter host
        int ram = 128000; // RAM (MB)
        long storage = 1000000; // Storage (MB)
        int bw = 10000; // Bandwidth (Mb/s)
        int maxpower = 117; // Konsumsi daya maksimum
        int staticPowerPercentage = 50; // Persentase daya statis

        // Membuat host dan menambahkannya ke daftar host
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

        // Karakteristik pusat data
        String arch = "x86"; // Arsitektur
        String os = "Linux"; // Sistem operasi
        String vmm = "Xen"; // Virtual Machine Monitor
        double time_zone = 10.0; // Zona waktu
        double cost = 3.0; // Biaya penggunaan per jam
        double costPerMem = 0.05; // Biaya per GB RAM
        double costPerStorage = 0.1; // Biaya per GB storage
        double costPerBw = 0.1; // Biaya per Gb/s bandwidth
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        // Membuat karakteristik pusat data
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // Membuat pusat data
        PowerDatacenter datacenter = null;
        try {
            datacenter = new PowerDatacenter(name, characteristics, new PowerVmAllocationPolicySimple(hostList), storageList, 9); 
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    /**
     * Membuat broker data center
     * 
     * Broker data center bertanggung jawab untuk mengelola VM dan cloudlet
     * untuk pengguna.
     * 
     * @return DatacenterBroker Broker yang dibuat
     */
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

    /**
     * Mencetak daftar cloudlet dan statistiknya
     * 
     * Metode ini mencetak informasi tentang cloudlet yang telah diproses
     * dan menghitung berbagai metrik kinerja.
     * 
     * @param list Daftar cloudlet yang telah diproses
     * @param trialNum Nomor percobaan saat ini
     */
    private static void printCloudletList(List<Cloudlet> list, int trialNum) throws FileNotFoundException {
        // Inisialisasi output yang dicetak ke nol
        int size = list.size();
        Cloudlet cloudlet = null;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT TRIAL " + trialNum + " ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
            "Data center ID" + indent + "VM ID" + indent + "Time"
            + indent + "Start Time" + indent + "Finish Time" + indent + "Waiting Time");

        // Variabel untuk menghitung statistik
        double waitTimeSum = 0.0;
        double CPUTimeSum = 0.0;
        int totalValues = 0;
        DecimalFormat dft = new DecimalFormat("###,##");

        double response_time[] = new double[size];

        // Mencetak status semua cloudlet
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(cloudlet.getCloudletId() + indent + indent);

            // Jika cloudlet berhasil diproses
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
        
        // Menghitung statistik untuk waktu respons
        DoubleSummaryStatistics stats = DoubleStream.of(response_time).summaryStatistics();

        // Mencetak berbagai metrik kinerja
        Log.printLine();
        System.out.println(String.format("min = %,6f",stats.getMin()));
        System.out.println(String.format("Response_Time: %,6f",CPUTimeSum / totalValues));

        Log.printLine();
        Log.printLine(String.format("TotalCPUTime : %,6f",CPUTimeSum));
        Log.printLine("TotalWaitTime : "+waitTimeSum);
        Log.printLine("TotalCloudletsFinished : "+totalValues);

        // Rata-rata Cloudlet Selesai
        Log.printLine(String.format("AverageCloudletsFinished : %,6f",(CPUTimeSum / totalValues)));

        // Rata-rata Waktu Mulai
        double totalStartTime = 0.0;
        for (int i = 0; i < size; i++) {
            totalStartTime += cloudletList.get(i).getExecStartTime();
        }
        double avgStartTime = totalStartTime / size;
        System.out.println(String.format("Average StartTime: %,6f",avgStartTime));

        // Rata-rata Waktu Eksekusi
        double ExecTime = 0.0;
        for (int i = 0; i < size; i++) {
            ExecTime += cloudletList.get(i).getActualCPUTime();
        }
        double avgExecTime = ExecTime / size;
        System.out.println(String.format("Average Execution Time: %,6f",avgExecTime));

        // Rata-rata Waktu Selesai
        double totalTime = 0.0;
        for (int i = 0; i < size; i++) {
            totalTime += cloudletList.get(i).getFinishTime();
        }
        double avgTAT = totalTime / size;
        System.out.println(String.format("Average FinishTime: %,6f",avgTAT));

        // Rata-rata Waktu Tunggu
        double avgWT = cloudlet.getWaitingTime() / size;
        System.out.println(String.format("Average Waiting time: %,6f",avgWT));

        // Throughput (Jumlah cloudlet yang diproses per unit waktu)
        double maxFT = 0.0;
        for (int i = 0; i < size; i++) {
            double currentFT = cloudletList.get(i).getFinishTime();
            if (currentFT > maxFT) {
                maxFT = currentFT;
            }
        }
        double throughput = size / maxFT;
        System.out.println(String.format("Throughput: %,9f",throughput));

        // Makespan (Waktu total untuk menyelesaikan semua cloudlet)
        double makespan = 0.0;
        double makespan_total = makespan + cloudlet.getFinishTime();
        System.out.println(String.format("Makespan: %,f",makespan_total));

        // Degree of Imbalance (Ketidakseimbangan penggunaan sumber daya)
        double degree_of_imbalance = (stats.getMax() - stats.getMin()) / (CPUTimeSum / totalValues);
        System.out.println(String.format("Imbalance Degree: %,3f",degree_of_imbalance));

        // Panjang Penjadwalan Total
        double scheduling_length = waitTimeSum + makespan_total;
        Log.printLine(String.format("Total Scheduling Length: %,f", scheduling_length));

        // Pemanfaatan Sumber Daya CPU
        double resource_utilization = (CPUTimeSum / (makespan_total * 54)) * 100;
        Log.printLine(String.format("Resource Utilization: %,f",resource_utilization));

        // Konsumsi Energi
        double energyConsumption = (datacenter1.getPower() + datacenter2.getPower() + datacenter3.getPower() + 
                                  datacenter4.getPower() + datacenter5.getPower() + datacenter6.getPower()) / (3600 * 1000);
        Log.printLine(String.format("Total Energy Consumption: %,2f  kWh", energyConsumption));
        
        // Menulis hasil ke file CSV
        try {
            csvWriter.write(String.format("%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\n",
                trialNum,
                waitTimeSum,
                avgStartTime,
                avgExecTime,
                avgTAT,
                throughput,
                makespan_total,
                degree_of_imbalance,
                scheduling_length,
                resource_utilization,
                energyConsumption));
            csvWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}