package com.example.neighborhood.model;

public final class Venue {
  public String id;
  public String name;
  public int capacity;
  public boolean allowsSpeaker;
  public String rules;

  public Venue(String id, String name, int capacity, boolean allowsSpeaker, String rules) {
    this.id = id;
    this.name = name;
    this.capacity = capacity;
    this.allowsSpeaker = allowsSpeaker;
    this.rules = rules;
  }

  @Override
  public String toString() {
    return name;
  }
}
