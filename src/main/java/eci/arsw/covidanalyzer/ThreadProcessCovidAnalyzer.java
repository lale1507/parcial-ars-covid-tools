package eci.arsw.covidanalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sun.awt.Mutex;

public class ThreadProcessCovidAnalyzer extends Thread{
    CovidAnalyzerTool covidAnalyzerTool;
    ResultAnalyzer resultAnalyzer;
    TestReader testReader;
    ArrayList<File> archivos;
    boolean detenido;
    Mutex mutex;

    //Constructor
    public ThreadProcessCovidAnalyzer(CovidAnalyzerTool covidAnalyzerTool, ResultAnalyzer resultAnalyzer,TestReader testReader){
        this.covidAnalyzerTool = covidAnalyzerTool;
        this.resultAnalyzer = resultAnalyzer;
        this.testReader = testReader;
        this.archivos = new ArrayList<File>();
        this.mutex = new Mutex();
        detenido = false;
    }

    @Override
    public void run(){
        for(File archivo: archivos){
            List<Result> resultados = testReader.readResultsFromFile(archivo);
            for(Result transaction: resultados) {
                while (detenido) {
                    synchronized (mutex) {
                        try {
                            mutex.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    resultAnalyzer.addResult(transaction);
                }

            }
            covidAnalyzerTool.processArchive();
        }

    }


    public void addArchivo(File files) {
        archivos.add(files);
    }

    public void pause() {
        detenido = true;
    }

    public void continues() {
        detenido = false;
        synchronized (mutex){
            mutex.notify();
        }
    }
}
