package com.simplify.android.sdk;

@SuppressWarnings("unused")
public class Customer {

    String id;
    String name;
    String email;


    /**
     * The id of this customer
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * The customer name
     * @return The customer name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the customer name
     * @param name The customer name
     * @return The customer
     */
    public Customer setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * The customer email address
     * @return The email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the customer email address
     * @param email The email address
     * @return The customer
     */
    public Customer setEmail(String email) {
        this.email = email;
        return this;
    }
}
