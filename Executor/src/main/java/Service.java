import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Student;
import org.hibernate.HibernateException;


import javax.persistence.*;
import java.io.*;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Service {
    private Properties p = new Properties();
    private final File studentDir;
    private final File inProgress;
    private final File archived;
    private final String folderPath;
    private final String fileNameRegex;
    private final int readerThreads;
    private final int execThreads;
    private final int batchSize;
    final ScheduledExecutorService service;
    final private EntityManagerFactory sessionFactory;

    public Service(String folder) throws IOException {
        this.sessionFactory = Persistence.createEntityManagerFactory("studentPU");
        this.p.load(new FileReader(folder));
        this.execThreads = Integer.parseInt(this.p.getProperty("exec_threads"));
        this.readerThreads = Integer.parseInt(this.p.getProperty("reader_threads"));
        this.fileNameRegex = this.p.getProperty("file_name_regex");
        this.folderPath = this.p.getProperty("folder_path");
        this.batchSize = Integer.parseInt(this.p.getProperty("batch_size"));

        this.studentDir = new File(this.folderPath);
        if(!studentDir.isDirectory()) {
            throw new NotDirectoryException(folderPath + " is not a directory.");
        }
        this.inProgress = new File(this.studentDir.getParent() + File.separator + "In Progress");
        this.inProgress.mkdirs();
        this.archived = new File(this.studentDir.getParent() + File.separator + "Archived");
        this.archived.mkdirs();
        this.service = Executors.newScheduledThreadPool(this.readerThreads);
    }

    public void start() {
        for(int i = 0; i < 1; i++){
            service.scheduleAtFixedRate(new checkFolder(), 0, 1000, MILLISECONDS);
        }
    }

    private class checkFolder implements Runnable {
        @Override
        public void run() {
            synchronized (studentDir){
                for(File f: studentDir.listFiles()){
                    if(f.getName().matches(fileNameRegex)){
                        File newLocation = new File(inProgress + File.separator + f.getName());
                        if(f.renameTo(newLocation)){
                            fileExecute(newLocation);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void fileExecute(File f) {
        final ExecutorService service = Executors.newFixedThreadPool(1);
        int i = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(f))){
            String currentLine;
            List<String> lines = new ArrayList<>();
            while((currentLine = reader.readLine()) != null){
                lines.add(currentLine.strip());
                i++;
                if(i == batchSize){
                    service.execute(new processLines(lines));
                    i = 0;
                    lines = new ArrayList<>();
                }
            }
            if(i > 0){
                service.submit(new processLines(lines));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class processLines implements Runnable{
        EntityManager em = sessionFactory.createEntityManager();
        private final List<String> toProcess;
        public processLines(List<String> l){
            this.toProcess = l;
        }
        @Override
        public void run() {
            this.toProcess.forEach(s -> {
                Student student = null;
                try {
                    student = stringToPOJO(s);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                EntityTransaction tx = em.getTransaction();
                tx.begin();
                em.merge(student);
                em.flush();
                tx.commit();
            });
            em.close();
        }
    }

    private Student stringToPOJO(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<>() {});
    }

}

