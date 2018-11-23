package com.example.observableinputstreamtest;

import org.apache.commons.io.IOUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.concurrent.CompletableFuture;


@Controller
@SpringBootApplication
public class ObservableInputstreamTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObservableInputstreamTestApplication.class, args);
    }


    @GetMapping("/")
    public void main(HttpServletResponse response) throws IOException {

        try (InputStream inputStream = new ClassPathResource("/big.txt").getInputStream();
             ServletOutputStream outputStream = response.getOutputStream()
        ) {

            DoorayObservableInputStream observableInputStream = new DoorayObservableInputStream(inputStream);
            NewObserver pObserver = new NewObserver();
            observableInputStream.add(pObserver);

            pObserver.getCf()
                    .thenAcceptAsync(v -> {
                        System.out.println("\n");
                        System.out.println(v);
                    });

            IOUtils.copy(observableInputStream, outputStream);
            response.flushBuffer();
        }
    }

    public static class NewObserver extends DoorayObservableInputStream.Observer {

        private PipedOutputStream pipedOutputStream;
        private CompletableFuture<String> cf;

        public NewObserver() {
            try {
                this.pipedOutputStream = new PipedOutputStream();
                PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);

                this.cf = CompletableFuture.supplyAsync(() -> {
                    try {
                        IOUtils.copy(pipedInputStream, System.out);
                        return "success";
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return "fail";
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void data(int pByte) throws IOException {
            pipedOutputStream.write(pByte);
        }

        @Override
        public void data(byte[] pBuffer, int pOffset, int pLength) throws IOException {
            pipedOutputStream.write(pBuffer, pOffset, pLength);
        }

        @Override
        public void finished() throws IOException {
            super.finished();
            this.pipedOutputStream.close();

        }

        @Override
        void closed() throws IOException {
            super.closed();
            this.pipedOutputStream.close();

        }

        public CompletableFuture getCf() {
            return this.cf;
        }


    }


}
