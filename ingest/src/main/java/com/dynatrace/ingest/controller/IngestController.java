package com.dynatrace.ingest.controller;

import com.dynatrace.ingest.model.Book;
import com.dynatrace.ingest.model.Client;
import com.dynatrace.ingest.model.Ingest;
import com.dynatrace.ingest.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingest")
public class IngestController {
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private StorageRepository storageRepository;
    private BookstoreDataGenerator bookstoreDataGenerator = new BookstoreDataGenerator();
    public static boolean isIsWorking() {
        return isWorking;
    }

    static private boolean isWorking = false;

    private final Logger logger = LoggerFactory.getLogger(IngestController.class);

    // make a payment
    @PostMapping("")
    @Operation(summary = "Start/Stop data generator")
    public Ingest generateData(@RequestBody Ingest ingest) {
        ingest.setCode(200);
        if (IngestController.isWorking || bookstoreDataGenerator.isAlive()) {
            IngestController.isWorking = false;
            bookstoreDataGenerator.interrupt();
            try {
                bookstoreDataGenerator.join();
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage());
            }
            ingest.setMessage("Stopping the Data Generator...");
        } else {
            if (bookstoreDataGenerator.isInterrupted() || bookstoreDataGenerator.wasUsed()) {
                bookstoreDataGenerator = new BookstoreDataGenerator();
            }
            IngestController.isWorking = true;
            ingest.setMessage(ingest.isContinuousLoad() ? "Generation In Loop Started" : "One-time Generation Started");
            bookstoreDataGenerator.generateData(ingest, ingest.isContinuousLoad());
        }

        return ingest;
    }

    @PostMapping("/books")
    @Operation(summary = "Run generators for books only (all other params are ignored)")
    public Ingest createBooks(@RequestBody Ingest ingest) {
        ingest.setCode(200);
        logger.info("Generate books");
        bookstoreDataGenerator.booksGenerator(ingest);
        ingest.setMessage("Ok");
        return ingest;
    }

    @PostMapping("/clients")
    @Operation(summary = "Run generators for clients only (all other params are ignored)")
    public Ingest createClients(@RequestBody Ingest ingest) {
        ingest.setCode(200);
        logger.info("Generate clients");
        for (int i = 0; i < ingest.getNumClients(); i++) {
            clientRepository.create();
        }
        ingest.setMessage("Ok");
        return ingest;
    }

    @PostMapping("/storage")
    @Operation(summary = "Run generators for storage only. Books and clients must be already created (all other params are ignored)")
    public Ingest ingestStorage(@RequestBody Ingest ingest) {
        ingest.setCode(200);
        if (IngestController.isWorking) {
            IngestController.isWorking = false;
            ingest.setMessage("Generation Stopped");
            return ingest;
        }
        logger.info("Generate storage");
        for (int i = 0; i < ingest.getNumStorage(); i++) {
            storageRepository.create();
        }
        ingest.setMessage("Ok");
        return ingest;
    }

    @PostMapping("/carts")
    @Operation(summary = "Run generators for carts only. Books and clients must be already created (all other params are ignored)")
    public Ingest createCarts(@RequestBody Ingest ingest) {
        ingest.setCode(200);
        if (IngestController.isWorking) {
            IngestController.isWorking = false;
            ingest.setMessage("Generation Stopped");
            return ingest;
        }
        logger.info("Generate cart");
        for (int i = 0; i < ingest.getNumCarts(); i++) {
            cartRepository.create();
        }
        ingest.setMessage("Ok");
        return ingest;
    }

    @PostMapping("/orders")
    @Operation(summary = "Run generators for orders only. Books, clients and storage must be already created (all other params are ignored)")
    public Ingest createOrders(@RequestBody Ingest ingest) {
        ingest.setCode(200);
        if (IngestController.isWorking) {
            IngestController.isWorking = false;
            ingest.setMessage("Generation Stopped");
            return ingest;
        }
        logger.info("Generate orders");
        for (int i = 0; i < ingest.getNumOrders(); i++) {
            orderRepository.create();
        }
        ingest.setCode(200);
        ingest.setMessage("Ok");
        return ingest;
    }

    @PostMapping("/orders/submit")
    @Operation(summary = "Submit Orders that had been created (all other params are ignored)")
    public Ingest submitOrders(@RequestBody Ingest ingest) {
        ingest.setCode(200);
        if (IngestController.isWorking) {
            IngestController.isWorking = false;
            ingest.setMessage("Generation Stopped");
            return ingest;
        }
        logger.info("Generate order pay");
        for (int i = 0; i < ingest.getNumSubmitOrders(); i++) {
            orderRepository.update(null); // random order
        }
        ingest.setCode(200);
        ingest.setMessage("Ok");
        return ingest;
    }

    @PostMapping("/orders/cancel")
    @Operation(summary = "Cancel (reverse to sbumit) Orders that had been created (all other params are ignored)")
    public Ingest cancelOrders(@RequestBody Ingest ingest) {
        ingest.setCode(200);
        if (IngestController.isWorking) {
            IngestController.isWorking = false;
            ingest.setMessage("Generation Stopped");
            return ingest;
        }
        logger.info("Generate order cancel");
        for (int i = 0; i < ingest.getNumSubmitOrders(); i++) {
            orderRepository.update(null); // random order
        }
        ingest.setCode(200);
        ingest.setMessage("Ok");
        return ingest;
    }

    @PostMapping("/ratings")
    @Operation(summary = "Run generators for clients only (all other params are ignored)")
    public Ingest createRatings(@RequestBody Ingest ingest) {
        ingest.setCode(200);
        if (IngestController.isWorking) {
            IngestController.isWorking = false;
            ingest.setMessage("Generation Stopped");
            return ingest;
        }
        logger.info("Generate ratings");
        for (int i = 0; i < ingest.getNumRatings(); i++) {
            ratingRepository.create();
        }
        ingest.setCode(200);
        ingest.setMessage("Ok");
        return ingest;
    }


    @DeleteMapping("")
    @Operation(summary = "Delete all generated data from databases of all services")
    public void deleteData() {
        if (IngestController.isWorking) {
            IngestController.isWorking = false;
            return;
        }
        logger.info("Clear Data");
        bookstoreDataGenerator.clearData(true, true);
    }

    @GetMapping("/status")
    @Operation(summary = "Returns data-generation status as a string")
    public String getGenerationStatus() {
        String result;
        if (IngestController.isWorking && bookstoreDataGenerator.isAlive()) {
            result = "Data Generation is in progress";
        } else if (IngestController.isWorking && !bookstoreDataGenerator.isAlive()) {
            IngestController.isWorking = false;
            result = "Data Generation is dead";
        } else if (!IngestController.isWorking && bookstoreDataGenerator.isAlive()) {
            result = "Data Generation is being stopped...";
        } else {
            result = "Data Generation is OFF";
        }
        logger.info(result);
        return result;
    }


    /**
     * Bookstore data generator subclass. Designed to generate the data in background threads
     */
    private class BookstoreDataGenerator extends Thread {
        private Ingest ingest;
        private boolean loop;
        private boolean wasUsed = false;

        private void booksGenerator(@RequestBody Ingest ingest) {
            logger.info("Generate Books");
            // always generate one extra book ( i <= .. in the for loops)
            for (int i = 0; i <= ingest.getNumBooksVend(); i++) {
                if (ingest.isRandomPrice()) {
                    bookRepository.create(true);
                } else {
                    bookRepository.create(true, 12);
                }
            }
            for (int i = 0; i <= ingest.getNumBooksNotvend(); i++) {
                if (ingest.isRandomPrice()) {
                    bookRepository.create(false);
                } else {
                    bookRepository.create(false, 12);
                }
            }
            for (int i = 0; i <= ingest.getNumBooksRandVend(); i++) {
                if (ingest.isRandomPrice()) {
                    bookRepository.create();
                } else {
                    bookRepository.create(12);
                }
            }
        }

        public void clearData(boolean clearBooksAndClients, boolean clearRatings) {
            if (clearRatings) {
                logger.info("Clearing ratings");
                ratingRepository.deleteAll();
            }
            logger.info("Clearing orders");
            orderRepository.deleteAll();
            logger.info("Clearing carts");
            cartRepository.deleteAll();
            logger.info("Clearing storage");
            storageRepository.deleteAll();

            if (clearBooksAndClients) {
                logger.info("Clearing clients");
                clientRepository.deleteAll();
                logger.info("Clearing books");
                bookRepository.deleteAll();
            }
        }

        public void generateData(Ingest ingest, boolean inLoop) {
            if (!IngestController.isWorking) {
                logger.info("Stopping Generator");
                return;
            }
            this.ingest = ingest;
            this.loop = inLoop;
            this.wasUsed = true;
            this.start();
        }

        public void run() {
            if (this.ingest == null) {
                return;
            }
            while (IngestController.isWorking) {
                this.generate(this.ingest);

                if (!this.loop) {
                    IngestController.isWorking = false;
                    this.interrupt();
                    break;
                }
            }
        }

        private void generate(Ingest ingest) {
            logger.info("Generate Data");
            if (ingest.getNumBooksRandVend() + ingest.getNumBooksVend() + ingest.getNumBooksNotvend() < ingest.getNumStorage()) {
                ingest.setNumStorage(ingest.getNumBooksRandVend() + ingest.getNumBooksVend() + ingest.getNumBooksNotvend());
            }
            logger.info("clearing data");
            boolean regenerateBooksAndClients = ingest.getNumBooks() > Book.getNumOfISBNs() || ingest.getNumClients() > Client.getNumOfClients();
            clearData(regenerateBooksAndClients, false); // let's keep ratings queryable in the GUI till the other data is being generated

            if (IngestController.isWorking && regenerateBooksAndClients) {
                logger.info("books");
                booksGenerator(ingest);
                for (int i = 0; i < ingest.getNumClients(); i++) {
                    logger.info("clients");
                    clientRepository.create();
                }
            }
            for (int i = 0; IngestController.isWorking && i < ingest.getNumCarts(); i++) {
                logger.info("carts");
                cartRepository.create();
            }
            for (int i = 0; IngestController.isWorking && i < ingest.getNumStorage(); i++) {
                logger.info("storage");
                storageRepository.create();
            }
            for (int i = 0; IngestController.isWorking && i < ingest.getNumOrders(); i++) {
                logger.info("orders");
                orderRepository.create();
            }
            for (int i = 0; IngestController.isWorking && i < ingest.getNumSubmitOrders(); i++) {
                logger.info("pay orders");
                orderRepository.update(null); // random order
            }
            // clearing ratings now
            logger.info("Clearing ratings");
            ratingRepository.deleteAll();
            for (int i = 0; IngestController.isWorking && i < ingest.getNumRatings(); i++) {
                logger.info("ratings");
                ratingRepository.create();
            }
        }

        public boolean wasUsed() {
            return this.wasUsed;
        }
    }
}
