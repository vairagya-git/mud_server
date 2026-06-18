package com.rama.mudstock.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

@Entity
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ticker")
    private String ticker;
    @Column(length = 255)
    private String name;
    @Column(length = 32, nullable = false)
    private String cusip;
    @Column(length = 20, nullable = false)
    private String cik;
    @Column(name = "cl", length = 32, nullable = false)
    private String cl;
    @Column(length = 64, nullable = false)
    private String country;
    @Column(name = "investor_page", length = 512)
    private String investorPage;
    @Column(length = 128)
    private String sector;

    @ManyToMany(mappedBy = "stocks")
    private Set<Watchlist> watchlists = new HashSet<>();

    public Stock() {}
    public Stock(String ticker) {
        this.ticker = ticker;
    }

    public Stock(String ticker, String name, String cusip, String cik, String cl, String country) {
        this.ticker = ticker;
        this.name = name;
        this.cusip = cusip;
        this.cik = cik;
        this.cl = cl;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCusip() {
        return cusip;
    }

    public void setCusip(String cusip) {
        this.cusip = cusip;
    }

    public String getCik() {
        return cik;
    }

    public void setCik(String cik) {
        this.cik = cik;
    }

    public String getCl() {
        return cl;
    }

    public void setCl(String cl) {
        this.cl = cl;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getInvestorPage() {
        return investorPage;
    }

    public void setInvestorPage(String investorPage) {
        this.investorPage = investorPage;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public Set<Watchlist> getWatchlists() {
        return watchlists;
    }

    public void setWatchlists(Set<Watchlist> watchlists) {
        this.watchlists = watchlists;
    }

    public void addWatchlist(Watchlist w) {
        this.watchlists.add(w);
        if (!w.getStocks().contains(this)) {
            w.getStocks().add(this);
        }
    }

    public void removeWatchlist(Watchlist w) {
        this.watchlists.remove(w);
        w.getStocks().remove(this);
    }

    
}
