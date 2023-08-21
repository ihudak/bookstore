package com.dynatrace.orders.controller;

import com.dynatrace.controller.HardworkingController;
import com.dynatrace.exception.*;
import com.dynatrace.orders.model.*;
import com.dynatrace.model.*;
import com.dynatrace.orders.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController extends HardworkingController {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private StorageRepository storageRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private ConfigRepository configRepository;
    private Logger logger = LoggerFactory.getLogger(OrderController.class);


    // get all Orders
    @GetMapping("")
    @Operation(summary = "Get all orders")
    public List<Order> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.ASC, "email", "createdAt"));
    }

    // get Order by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get an order by ID")
    public Order getOrderById(@PathVariable Long id) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Order not found");
            logger.error(ex.getMessage());
            throw ex;
        }
        return order.get();
    }

    // get Orders of a user
    @GetMapping("/findByEmail")
    @Operation(summary = "Find all orders for the given client")
    public List<Order> getOrdersByEmail(@Parameter(name="email", description = "email of an existing client", example = "pbrown.gmail.com") @RequestParam String email) {
        logger.info("Finding orders for user " + email);
        this.verifyClient(email);
        return orderRepository.findByEmail(email);
    }

    // get all users who ordered the book
    @GetMapping("/findByISBN")
    @Operation(summary = "Find all orders for the given book")
    public List<Order> getOrdersByISBN(@Parameter(name="isbn", description = "ISBN13, digits only (no dashes, no spaces)", example = "9783161484100") @RequestParam String isbn) {
        logger.info("Finding orders for book " + isbn);
        this.verifyBook(isbn);
        return orderRepository.findByEmail(isbn);
    }

    // create an order
    @PostMapping("")
    @Operation(summary = "Create a new order")
    public Order createOrder(@RequestBody Order order) {
        simulateHardWork();
        simulateCrash();
        logger.info("client " + order.getEmail() + " orders book " + order.getIsbn());
        Book book = verifyBook(order.getIsbn());
        order.setPrice(book.getPrice()); // new order - taking the fresh price
        verifyClient(order.getEmail());
        Storage storage;
        try {
            storage = verifyStorage(order.getIsbn(), order.getQuantity());
        } catch (InsufficientResourcesException ex) {
            logger.error(ex.getMessage());
            throw ex;
        }
        if (order.isCompleted()) {
            buyFromStorage(storage, order, book);
        }
        // check if such order already exists under a different id
        Order existingOrder = orderRepository.findByEmailAndIsbn(order.getEmail(), order.getIsbn());
        if (existingOrder != null && order.getId() != existingOrder.getId()) {
            order.setId(existingOrder.getId());
        }
        logger.debug("Created order for book " + order.getIsbn() + " client " + order.getEmail());
        return orderRepository.save(order);
    }

    // update an order
    @PutMapping("/{id}")
    @Operation(summary = "Update the order")
    public Order updateOrderById(@PathVariable Long id, @RequestBody Order order) {
        logger.info("Updating order " + order.getIsbn() + " of client " + order.getEmail());
        Optional<Order> orderDb = orderRepository.findById(id);
        if (orderDb.isEmpty()) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Order not found");
            logger.error(ex.getMessage());
            throw ex;
        } else if (order.getId() != id || orderDb.get().getId() != id) {
            BadRequestException ex = new BadRequestException("bad order id");
            logger.error(ex.getMessage());
            throw ex;
        }

        Storage storage;
        try {
            storage = verifyStorage(order.getIsbn(), order.getQuantity());
        } catch (InsufficientResourcesException ex) {
            logger.error(ex.getMessage());
            throw ex;
        }
        if (order.isCompleted() && !orderDb.get().isCompleted()) {
            Book book = verifyBook(order.getIsbn());
            // complete the order
            buyFromStorage(storage, order, book);
        } else if (!order.isCompleted() && orderDb.get().isCompleted()) {
            // cancel the order
            returnToStorage(storage, order);
        }

        return orderRepository.save(order);
    }

    // submit order
    @PostMapping("/submit")
    @Operation(summary = "Try submitting the order (client must exist, book must exist and be vendible, storage must contain enough pieces, payment must succeed")
    public Order submitOrder(@RequestBody Order order) {
        simulateHardWork();
        simulateCrash();
        logger.info("Submitting order " + order.getIsbn() + " client " + order.getEmail());
        Order orderDb = orderRepository.findByEmailAndIsbn(order.getEmail(), order.getIsbn());
        if (null == orderDb) {
            logger.error("Order not found, ISBN " + order.getIsbn() + " client " + order.getEmail());
            orderDb = order; // making a new order then
        } else if (orderDb.isCompleted()) {
            logger.error("Order is already paid, ISBN " + order.getIsbn() + " client " + order.getEmail());
            return orderDb; // no need to buy again
        }
        verifyClient(order.getEmail());
        Book book = verifyBook(order.getIsbn());
        Storage storage;
        try {
            storage = verifyStorage(order.getIsbn(), order.getQuantity());
        } catch (InsufficientResourcesException ex) {
            logger.error(ex.getMessage());
            throw ex;
        }

        orderDb.setQuantity(order.getQuantity());

        buyFromStorage(storage, orderDb, book);
        logger.debug("Submitted order for book " + order.getIsbn() + " client " + order.getEmail());
        return orderRepository.save(orderDb);
    }

    // cancel order
    @PostMapping("/cancel")
    @Operation(summary = "Cancel order (books will be returned to the storage)")
    public Order cancelOrder(@RequestBody Order order) {
        simulateHardWork();
        simulateCrash();
        logger.info("Canceling order " + order.getIsbn() + " client " + order.getEmail());
        Order orderDb = orderRepository.findByEmailAndIsbn(order.getEmail(), order.getIsbn());
        if (null == orderDb) {
            BadRequestException ex = new BadRequestException("Order not found, ISBN " + order.getIsbn() + " client " + order.getEmail());
            logger.error(ex.getMessage());
            throw ex;
        }
        if (!orderDb.isCompleted()) {
            AlreadyPaidException ex = new AlreadyPaidException("Order is not paid, ISBN " + order.getIsbn() + " client " + order.getEmail());
            logger.error(ex.getMessage());
            throw ex;
        }
        verifyClient(order.getEmail());
        Storage storage;
        try {
            storage = verifyStorage(order.getIsbn(), (-1) * order.getQuantity());
        } catch (InsufficientResourcesException ex) {
            logger.error(ex.getMessage());
            throw ex;
        }
        orderDb.setQuantity(order.getQuantity());

        returnToStorage(storage, orderDb);
        logger.debug("Canceled order for book " + order.getIsbn() + " client " + order.getEmail());
        return orderRepository.save(orderDb);
    }

    // delete an order
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an order by ID. If the order had been processed, this operation DOES NOT return books to the storage")
    public void deleteOrderById(@PathVariable Long id) {
        logger.info("Deleting order " + id.toString());
        orderRepository.deleteById(id);
    }

    // delete all orders
    @DeleteMapping("/delete-all")
    @Operation(summary = "Delete all orders")
    public void deleteAllBooks() {
        logger.info("Deleting all orders");
        orderRepository.truncateTable();
    }

    private void verifyClient(String email) {
        logger.info("Verifying client " + email);
        Client client = clientRepository.getClientByEmail(email);
        if (null == client) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Client is not found by email " + email);
            logger.error(ex.getMessage());
            throw ex;
        }
        Client[] clients = clientRepository.getAllClients();
        logger.debug(clients.toString());
    }

    private Book verifyBook(String isbn) {
        logger.info("Verifying book " + isbn);
        Book book = bookRepository.getBookByISBN(isbn);
        if (null == book) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book not found by isbn " + isbn);
            logger.error(ex.getMessage());
            throw ex;
        }
        if (!book.isPublished()) {
            ResourceNotFoundException ex = new ResourceNotFoundException("The book is not yet vendible, ISBN: " + isbn);
            logger.error(ex.getMessage());
            throw ex;
        }
        Book[] books = bookRepository.getAllBooks();
        logger.debug(books.toString());
        return book;
    }

    private Storage verifyStorage(String isbn, int quantity) throws InsufficientResourcesException {
        logger.info("Verifying storage " + isbn);
        Storage storage = storageRepository.getStorageByISBN(isbn);
        if (quantity > 0 && (null == storage || storage.getQuantity() < quantity)) {
            InsufficientResourcesException ex = new InsufficientResourcesException("We do not have enough books in storage, ISBN: " + isbn);
            logger.error(ex.getMessage());
            throw ex;
        }
        Storage[] storages = storageRepository.getAllBooksInStorage();
        logger.debug(storages.toString());
        return storage;
    }

    private void buyFromStorage(Storage storage, Order order, Book book) {
        simulateHardWork();
        simulateCrash();
        logger.info("Buying from storage " + book.getIsbn() + " for client " + order.getEmail());
        if (!storage.getIsbn().equals(order.getIsbn())) {
            BadRequestException ex = new BadRequestException("Wrong storage for ISBN: " + order.getIsbn());
            logger.error(ex.getMessage());
            throw ex;
        }
        storage.setQuantity(order.getQuantity());
        if (!order.isCompleted()) {
            order.setCompleted(true);
        }
        if (book.getPrice() > order.getPrice()) {
            PurchaseForbiddenException ex = new PurchaseForbiddenException("Price changed for book ISBN: " + book.getIsbn());
            logger.error(ex.getMessage());
            throw ex;
        } else if (book.getPrice() < order.getPrice()) {
            order.setPrice(book.getPrice());
        }
        try {
            storageRepository.buyBook(storage);
        } catch (PurchaseForbiddenException purchaseForbiddenException) {
            order.setCompleted(false);
            throw purchaseForbiddenException;
        }
        try {
            payOrder(order);
        } catch (PaymentException paymentException) {
            logger.error("RETURNING BOOKS TO STORAGE");
            logger.error(paymentException.getMessage());
            storageRepository.returnBook(storage);
            order.setCompleted(false);
            throw paymentException;
        }
        logger.debug("Took from Storage book " + order.getIsbn() + " client " + order.getEmail());
    }

    private void returnToStorage(Storage storage, Order order) {
        simulateHardWork();
        simulateCrash();
        logger.info("Returning to storage " + order.getIsbn() + " for client " + order.getEmail());
        if (!storage.getIsbn().equals(order.getIsbn())) {
            BadRequestException ex = new BadRequestException("Wrong storage for ISBN: " + order.getIsbn());
            logger.error(ex.getMessage());
            throw ex;
        }
        if (order.isCompleted()) {
            order.setCompleted(false);
        }
        storage.setQuantity(order.getQuantity());
        try {
            storageRepository.returnBook(storage);
        } catch (PurchaseForbiddenException purchaseForbiddenException) {
            logger.error(purchaseForbiddenException.getMessage());
            order.setCompleted(true);
        }
        logger.debug("Returned order for book " + order.getIsbn() + " client " + order.getEmail());
    }

    private void payOrder(Order order) throws PaymentException {
        simulateHardWork();
        simulateCrash();
        logger.info("Paying order " + order.getIsbn() + " client " + order.getEmail());
        Payment payment = new Payment(order.getId(), order.getPrice() * order.getPrice(), order.getEmail());
        try {
            payment = paymentRepository.submitPayment(payment);
        } catch (RuntimeException ex) {
            logger.error("Payment Crashed: " + ex.getMessage());
            throw new PaymentException(ex.getMessage());
        }
        if (null == payment || !payment.isSucceeded()) {
            PaymentException ex = new PaymentException("Payment Failed: " + (null == payment ? "no response" : payment.getMessage()));
            logger.error("Payment Failed: " + ex.getMessage());
            throw new PaymentException(ex.getMessage());
        }
        logger.debug("Paid order for book " + order.getIsbn() + " client " + order.getEmail());
    }

    @Override
    public ConfigRepository getConfigRepository() {
        return configRepository;
    }
}
