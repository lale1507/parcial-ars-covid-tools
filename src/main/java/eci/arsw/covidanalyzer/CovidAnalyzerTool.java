package eci.arsw.covidanalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Camel Application
 */
public class CovidAnalyzerTool {

    private ResultAnalyzer resultAnalyzer;
    private TestReader testReader;
    private int amountOfFilesTotal;
    private AtomicInteger amountOfFilesProcessed;
    private int numHilos = 10;
    private static CovidAnalyzerTool covidAnalyzerTool = new CovidAnalyzerTool();
    private static boolean detenido = false;
    private ThreadProcessCovidAnalyzer[] arregloDeHilos;

    public CovidAnalyzerTool() {
        resultAnalyzer = new ResultAnalyzer();
        testReader = new TestReader();
        amountOfFilesProcessed = new AtomicInteger();

    }

    public void processResultData() {
        amountOfFilesProcessed.set(0);
        List<File> resultFiles = getResultFileList();
        arregloDeHilos = new ThreadProcessCovidAnalyzer[numHilos];

        for(int i=0; i < numHilos;i++){
            arregloDeHilos[i] = new ThreadProcessCovidAnalyzer(this, resultAnalyzer, testReader);
        }
        int resi = 0;
        amountOfFilesTotal = resultFiles.size();
        for(File files: resultFiles ){
            arregloDeHilos[resi].addArchivo(files);
            if (resi == numHilos-1){
                resi = 0;
            }else{
                resi = resi +1;
            }
        }
        for(ThreadProcessCovidAnalyzer hilos: arregloDeHilos){
            hilos.start();
        }
        for(ThreadProcessCovidAnalyzer hilos: arregloDeHilos ){
            try{
                hilos.join();
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }
        }

    }

    private List<File> getResultFileList() {
        List<File> csvFiles = new ArrayList<>();
        try (Stream<Path> csvFilePaths = Files.walk(Paths.get("src/main/resources/")).filter(path -> path.getFileName().toString().endsWith(".csv"))) {
            csvFiles = csvFilePaths.map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvFiles;
    }


    public Set<Result> getPositivePeople() {
        return resultAnalyzer.listOfPositivePeople();
    }

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        //CovidAnalyzerTool covidAnalyzerTool = new CovidAnalyzerTool();
        Thread processingThread = new Thread(() -> covidAnalyzerTool.processResultData());
        processingThread.start();
        while (true) {
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            if (line.contains("exit"))
                break;
            else if(line.equals("") && !detenido){
                System.out.println("Se pauso");
                detenido = true;
                for(ThreadProcessCovidAnalyzer hilos: covidAnalyzerTool.get()){
                    hilos.pause();
                }
                String message = "Processed %d out of %d files.\nFound %d positive people:\n%s";
                Set<Result> positivePeople = covidAnalyzerTool.getPositivePeople();
                String affectedPeople = positivePeople.stream().map(Result::toString).reduce("", (s1, s2) -> s1 + "\n" + s2);
                message = String.format(message, covidAnalyzerTool.amountOfFilesProcessed.get(), covidAnalyzerTool.amountOfFilesTotal, positivePeople.size(), affectedPeople);
                System.out.println(message);

            }else if(line.equals("") && detenido){
                detenido = false;
                System.out.println("Continuo");
                for(ThreadProcessCovidAnalyzer hilos: covidAnalyzerTool.get()){
                    hilos.continues();
                }
            }

        }
        String message = "Processed %d out of %d files.\nFound %d positive people:\n%s";
        Set<Result> positivePeople = covidAnalyzerTool.getPositivePeople();
        String affectedPeople = positivePeople.stream().map(Result::toString).reduce("", (s1, s2) -> s1 + "\n" + s2);
        message = String.format(message, covidAnalyzerTool.amountOfFilesProcessed.get(), covidAnalyzerTool.amountOfFilesTotal, positivePeople.size(), affectedPeople);
        System.out.println(message);
    }

    private ThreadProcessCovidAnalyzer[] get() {
        return arregloDeHilos;
    }

    public void processArchive() {
        amountOfFilesProcessed.incrementAndGet();
    }
}

