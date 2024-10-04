package africa.flot.domain.model.valueobject;

import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    public String street;
    public String city;
    public String state;
    public String postalCode;
    public String country;

    // Constructors
    public Address() {
    }

    public Address(String street, String city, String state, String postalCode, String country) {
        if (street == null || city == null || country == null) {
            throw new IllegalArgumentException("Address fields cannot be null.");
        }
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
}
