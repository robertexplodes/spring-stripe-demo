package com.example.springstripedemo.controller;

import com.example.springstripedemo.domain.Order;
import com.example.springstripedemo.domain.Product;
import com.example.springstripedemo.persistence.OrderRepository;
import com.example.springstripedemo.persistence.ProductRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.SneakyThrows;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
public class Controller {


    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public Controller(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/products")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/randomOrder/{name}")
    public Order getRandomOrder(@PathVariable String name) {
        var products = productRepository.findAll();
        var random = new Random();
        var randInt = random.nextInt(10 + 1) + 1;
        var order = new Order(null, name, new ArrayList<>(), false);
        for (int i = 0; i < randInt; i++) {
            order.addProduct(products.get(i % products.size()));
        }
        return orderRepository.save(order);
    }

    @SneakyThrows
    @PostMapping("/checkout/{id}")
    public ModelAndView checkout(@PathVariable Long id, ModelMap model) {
        var order = orderRepository.findById(id).orElseThrow();
        if(order.isPaid()) {
            throw new IllegalArgumentException("Order is already paid");
        }

        SessionCreateParams.Builder builder = new SessionCreateParams.Builder()
                .setSuccessUrl("http://localhost:8080/success")
                .setCancelUrl("http://localhost:8080/cancel")
                .setMode(SessionCreateParams.Mode.PAYMENT);

        for(var product : order.getProducts()) {
            var item = new SessionCreateParams.LineItem
                    .Builder()
                    .setQuantity(1L)
                    .setPrice(product.getPrice().toString())
                    .build();
            builder.addLineItem(item);
        }

        SessionCreateParams createParams = builder.build();
        Session session = Session.create(createParams);
        model.addAttribute("attribute", "forwardWithForwardPrefix");
        return new ModelAndView("forward:/" + session.getUrl(), model);
    }
}
