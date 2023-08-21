package com.dynatrace.ingest.model;

import com.dynatrace.ingest.model.Model;
import io.swagger.v3.oas.annotations.media.Schema;

public class Ingest implements Model {
    @Schema(name = "code", example = "500", requiredMode = Schema.RequiredMode.AUTO, description = "HTTP status/error code. 0 in requests")
    private int code;
    @Schema(name = "message", example = "success", requiredMode = Schema.RequiredMode.AUTO, description = "Status message. Empty in requests")
    private String message;
    @Schema(name = "numBooksVend", example = "500", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many vendible books to generate")
    private int numBooksVend;
    @Schema(name = "numBooksNotvend", example = "500", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many invendible books to generate")
    private int numBooksNotvend;
    @Schema(name = "numBooksRandVend", example = "2500", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many books to generate (random vend status)")
    private int numBooksRandVend;
    @Schema(name = "numClients", example = "1500", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many clients to generate")
    private int numClients;
    @Schema(name = "numCarts", example = "500", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many carts to generate")
    private int numCarts;
    @Schema(name = "numOrders", example = "500", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many non submitted orders (payment not processed) to generate")
    private int numOrders;
    @Schema(name = "numSubmitOrders", example = "100", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many orders to process (does not create new orders, just submits the generated ones)")
    private int numSubmitOrders;
    @Schema(name = "numRatings", example = "500", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many ratings to generate")
    private int numRatings;
    @Schema(name = "numStorage", example = "1500", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many storage items (unique ISBNs) to generate")
    private int numStorage;
    @Schema(name = "numBooksPerStorage", example = "20", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many pieces per ISBN to generate in a storage (0 stands for random)")
    private int numBooksPerStorage;
    @Schema(name = "numBooksPerOrder", example = "2", requiredMode = Schema.RequiredMode.REQUIRED, description = "How many pieces per ISBN to generate in an order (0 stands for random)")
    private int numBooksPerOrder;
    @Schema(name = "randomPrice", example = "true", requiredMode = Schema.RequiredMode.AUTO, description = "Whether the book prices should be random (non-random is 12)")
    private boolean randomPrice;
    @Schema(name = "continuousLoad", example = "true", requiredMode = Schema.RequiredMode.AUTO, description = "Whether to keep starting generator from beginning (infinite loop, until called again)")
    private boolean continuousLoad;

    static private boolean contLoad = false;
    public static void setContLoad(boolean contLoad) {
        Ingest.contLoad = contLoad;
    }

    public static boolean isContLoad() {
        return contLoad;
    }


    public Ingest() {
    }

    public Ingest(int code, String message, int numBooksVend, int numBooksNotvend, int numBooksRandVend, int numClients, int numCarts, int numOrders, int numSubmitOrders, int numRatings, int numStorage, int numBooksPerStorage, int numBooksPerOrder, boolean randomPrice, boolean continuousLoad) {
        this.code = code;
        this.message = message;
        this.numBooksVend = numBooksVend;
        this.numBooksNotvend = numBooksNotvend;
        this.numBooksRandVend = numBooksRandVend;
        this.numClients = numClients;
        this.numCarts = numCarts;
        this.numOrders = numOrders;
        this.numSubmitOrders = numSubmitOrders;
        this.numRatings = numRatings;
        this.numStorage = numStorage;
        this.numBooksPerStorage = numBooksPerStorage;
        this.numBooksPerOrder = numBooksPerOrder;
        this.randomPrice = randomPrice;
        this.continuousLoad = continuousLoad;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getNumBooksVend() {
        return numBooksVend;
    }

    public void setNumBooksVend(int numBooksVend) {
        this.numBooksVend = numBooksVend;
    }

    public int getNumBooksNotvend() {
        return numBooksNotvend;
    }

    @Schema(name = "numBooks", example = "3500", requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Response only: number of all books vendible and non-vendible")
    public int getNumBooks() { return numBooksVend + numBooksNotvend + numBooksRandVend; }

    public void setNumBooksNotvend(int numBooksNotvend) {
        this.numBooksNotvend = numBooksNotvend;
    }

    public int getNumBooksRandVend() {
        return numBooksRandVend;
    }

    public void setNumBooksRandVend(int numBooksRandVend) {
        this.numBooksRandVend = numBooksRandVend;
    }

    public int getNumClients() {
        return numClients;
    }

    public void setNumClients(int numClients) {
        this.numClients = numClients;
    }

    public int getNumCarts() {
        return numCarts;
    }

    public void setNumCarts(int numCarts) {
        this.numCarts = numCarts;
    }

    public int getNumOrders() {
        return numOrders;
    }

    public void setNumOrders(int numOrders) {
        this.numOrders = numOrders;
    }

    public int getNumSubmitOrders() {
        return numSubmitOrders;
    }

    public void setNumSubmitOrders(int numSubmitOrders) {
        this.numSubmitOrders = numSubmitOrders;
    }

    public int getNumRatings() {
        return numRatings;
    }

    public void setNumRatings(int numRatings) {
        this.numRatings = numRatings;
    }

    public int getNumStorage() {
        return numStorage;
    }

    public void setNumStorage(int numStorage) {
        this.numStorage = numStorage;
    }

    public int getNumBooksPerStorage() {
        return numBooksPerStorage;
    }

    public void setNumBooksPerStorage(int numBooksPerStorage) {
        this.numBooksPerStorage = numBooksPerStorage;
    }

    public int getNumBooksPerOrder() {
        return numBooksPerOrder;
    }

    public void setNumBooksPerOrder(int numBooksPerOrder) {
        this.numBooksPerOrder = numBooksPerOrder;
    }

    public boolean isRandomPrice() {
        return randomPrice;
    }

    public void setRandomPrice(boolean randomPrice) {
        this.randomPrice = randomPrice;
    }

    public boolean isContinuousLoad() {
        return continuousLoad;
    }

    public void setContinuousLoad(boolean continuousLoad) {
        this.continuousLoad = continuousLoad;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public String toString() {
        return "Books " + this.numBooksNotvend + this.numBooksVend + this.numBooksRandVend + " Clients " + this.numClients;
    }
}
