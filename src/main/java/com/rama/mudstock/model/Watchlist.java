package com.rama.mudstock.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@Entity
public class Watchlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

        @ManyToMany
        @JoinTable(name = "watchlist_stock",
            joinColumns = @JoinColumn(name = "watchlist_id"),
            inverseJoinColumns = @JoinColumn(name = "stock_id"))
        private Set<Stock> stocks = new HashSet<>();

    public Watchlist() {}

    public Watchlist(String name) { this.name = name; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<Stock> getStocks() { return stocks; }
    public void setStocks(Set<Stock> stocks) { this.stocks = stocks; }

    public void addStock(Stock s) {
        this.stocks.add(s);
        if (!s.getWatchlists().contains(this)) {
            s.getWatchlists().add(this);
        }
    }

    public void removeStock(Stock s) {
        this.stocks.remove(s);
        s.getWatchlists().remove(this);
    }
}
