package com.dynatrace.carts.controller;

import com.dynatrace.carts.model.Cart;
import com.dynatrace.controller.HardworkingController;
import com.dynatrace.exception.BadRequestException;
import com.dynatrace.exception.ResourceNotFoundException;
import com.dynatrace.model.Book;
import com.dynatrace.model.Client;
import com.dynatrace.carts.repository.BookRepository;
import com.dynatrace.carts.repository.CartRepository;
import com.dynatrace.carts.repository.ClientRepository;
import com.dynatrace.carts.repository.ConfigRepository;
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
@RequestMapping("/api/v1/carts")
public class CartController extends HardworkingController {
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private ConfigRepository configRepository;
    private Logger logger = LoggerFactory.getLogger(CartController.class);

    // get all Carts
    @GetMapping("")
    @Operation(summary = "Get all carts")
    public List<Cart> getAllCarts() {
        return cartRepository.findAll(Sort.by(Sort.Direction.ASC, "email"));
    }

    // get Cart by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get a cart by ID")
    public Cart getCartById(@PathVariable Long id) {
        simulateCrash();
        Optional<Cart> cart = cartRepository.findById(id);
        if (cart.isEmpty()) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Cart not found");
            logger.error(ex.getMessage());
            throw ex;
        }
        return cart.get();
    }

    // get Cart of a user
    @GetMapping("/findByEmail")
    @Operation(summary = "Get all carts for the given client")
    public List<Cart> getCartsByEmail(@Parameter(name="email", description = "email of an existing client", example = "pbrown.gmail.com") @RequestParam String email) {
        simulateCrash();
        this.verifyClient(email);
        return cartRepository.findByEmail(email);
    }

    // get all users who have the book in their cart
    @GetMapping("/findByISBN")
    @Operation(summary = "Get all carts for the given book")
    public List<Cart> getCartsByISBN(@Parameter(name="isbn", description = "ISBN13, digits only (no dashes, no spaces)", example = "9783161484100") @RequestParam String isbn) {
        simulateCrash();
        this.verifyBook(isbn);
        return cartRepository.findByEmail(isbn);
    }

    // add a book to the cart
    @PostMapping("")
    @Operation(summary = "Add books to a potentially existing cart (will create one if does not exist)")
    public Cart addToCart(@RequestBody Cart cart) {
        simulateHardWork();
        simulateCrash();
        this.verifyClient(cart.getEmail());
        this.verifyBook(cart.getIsbn());
        Cart cartDB = cartRepository.findByEmailAndIsbn(cart.getEmail(), cart.getIsbn());
        if (cartDB == null) {
            return cartRepository.save(cart);
        }
        // cart already exists
        // increase quantity if found
        cartDB.setQuantity(cartDB.getQuantity() <= 0 ? cart.getQuantity() : cartDB.getQuantity() + cart.getQuantity());
        logger.debug("Adding to cart book " + cartDB.getIsbn() + " for user " + cartDB.getEmail());
        return cartRepository.save(cartDB);
    }

    // update a Cart
    @PutMapping("")
    @Operation(summary = "Update a cart (must exist)")
    public Cart updateCart(@RequestBody Cart cart) {
        simulateHardWork();
        simulateCrash();
        this.verifyClient(cart.getEmail());
        this.verifyBook(cart.getIsbn());
        Cart cartByEmailIsbn = cartRepository.findByEmailAndIsbn(cart.getEmail(), cart.getIsbn());

        if (null == cartByEmailIsbn) {
            BadRequestException ex = new BadRequestException("You can change only quantity");
            logger.error(ex.getMessage());
            throw ex;
        }
        cart.setId(cartByEmailIsbn.getId());
        return cartRepository.save(cart);
    }

    // remove specified quantity
    @DeleteMapping("")
    @Operation(summary = "Delete a cart")
    public Cart reduceCart(@RequestBody Cart cart) {
        this.verifyClient(cart.getEmail());
        this.verifyBook(cart.getIsbn());
        Cart cartByEmailIsbn = cartRepository.findByEmailAndIsbn(cart.getEmail(), cart.getIsbn());
        if (null == cartByEmailIsbn) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Cart not found");
            logger.error(ex.getMessage());
            throw ex;
        }
        if (cart.getQuantity() >= cartByEmailIsbn.getQuantity()) {
            // deleting the cart
            cartRepository.delete(cart);
            return null;
        }
        cartByEmailIsbn.setQuantity(cartByEmailIsbn.getQuantity() - cart.getQuantity());
        return cartRepository.save(cartByEmailIsbn);
    }

    // remove all carts
    @DeleteMapping("/delete-all")
    @Operation(summary = "Delete all carts")
    public void deteteAllCarts() {
        cartRepository.truncateTable();
    }


    private void verifyClient(String email) {
        Client client = clientRepository.getClientByEmail(email);
        if (null == client) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Client is not found by email " + email);
            logger.error(ex.getMessage());
            throw ex;
        }
        Client[] clients = clientRepository.getAllClients();
        logger.debug(clients.toString());
    }

    private void verifyBook(String isbn) {
        Book book = bookRepository.getBookByISBN(isbn);
        if (null == book) {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book not found by isbn " + isbn);
            logger.error(ex.getMessage());
            throw ex;
        }
        Book[] books = bookRepository.getAllBooks();
        logger.debug(books.toString());
    }

    @Override
    public ConfigRepository getConfigRepository() {
        return configRepository;
    }
}
