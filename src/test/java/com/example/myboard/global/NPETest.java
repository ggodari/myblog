package com.example.myboard.global;

import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;
import org.junit.jupiter.api.Test;

import java.util.*;

public class NPETest {

    private class Order {
        private Long id;
        private Date date;
        private Customer customer;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Customer getCustomer() {
            return customer;
        }

        public void setCustomer(Customer customer) {
            this.customer = customer;
        }
    }

    private class Customer {
        private Long id;
        private String name;
        private Address address;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    private class Address {
        private String street;
        private String city;
        private String zipcode;

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZipcode() {
            return zipcode;
        }

        public void setZipcode(String zipcode) {
            this.zipcode = zipcode;
        }
    }

    private String getCityOfCustomerFromOrder(Order order) throws Exception {

//        Optional<Order> maybeOrder = Optional.ofNullable(Order order);
//        if(maybeOrder.isPresent()) {
//            Optional<Customer> maybeCustomer = Optional.ofNullable(maybeOrder.get().getCustomer());
//            if(maybeCustomer.isPresent()) {
//                Optional<Address> maybeAddress = Optional.ofNullable(maybeCustomer.get().getAddress());
//                if(maybeAddress.isPresent()) {
//                    Address address = maybeAddress.get();
//                    Optional<String> maybeCity = Optional.ofNullable(address.getCity());
//                    if(maybeCity.isPresent()) {
//                        return maybeCity.get();
//                    }
//                }
//            }
//        }
//
//        int length = Optional.ofNullable(getText()).map(text -> text.length()).orElse(0);
//

        Map<Integer, String> cites = new HashMap<>();
        cites.put(1, "Seoul");
        cites.put(2, "Busan");
        cites.put(3, "Daejeon");

        String city = cites.get(4); // return null
        int length = city == null ? 0 : city.length();
        System.out.println(length);

        Optional<String> maybeCity = Optional.ofNullable(cites.get(4));
        int length1 = maybeCity.map(city1 -> city1.length()).orElse(0);

        List<String> ci = Arrays.asList("Seoul", "Busan", "Daejeon");
        Optional<String> c2 = Optional.ofNullable(ci.get(3));
        int len = c2.map(cc -> cc.length()).orElse(0);


        return Optional.ofNullable(order)
                .filter(o -> o.getDate().getTime() > System.currentTimeMillis() - 1000)
                .map(o -> o.getCustomer())
                .map(customer -> customer.getAddress())
                .map(address -> address.getCity())
                .orElse("Seoul");

        //return "Seoul" ;
    }

    private String getText() {
        return "Hello";
    }

    @Test
    public void npeTest() throws Exception {
        Order order = new Order();
        String city = getCityOfCustomerFromOrder(order);
        System.out.println("city : " + city);
    }


    public static <T> Optional<T> getAsOptional(List<T> list, int index) {
        try {
            return Optional.of(list.get(index));
        } catch (ArrayIndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }
}
