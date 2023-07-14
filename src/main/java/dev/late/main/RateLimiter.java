package dev.late.main;

import java.util.LinkedList;
import java.util.Queue;

public class RateLimiter {

  private final int maxRate;
  private final Queue<Long> queue = new LinkedList<>();

  public RateLimiter(int maxRate) {
    this.maxRate = maxRate;
  }

  public synchronized boolean shouldAllow() {
    long currentTimeMillis = System.currentTimeMillis();

    if (queue.size() < maxRate) {
      queue.add(currentTimeMillis);
      return true;
    }

    long earliestTime = queue.peek();
    if (currentTimeMillis - earliestTime < 1000) {
      return false;
    } else {
      queue.poll();
      queue.add(currentTimeMillis);
      return true;
    }
  }
}
