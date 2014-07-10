package com.github.kevints.mesos;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.net.HostAndPort;

import static java.util.Objects.requireNonNull;

/**
 * An immutable representation of the location of a process.
 */
public final class PID implements Serializable {
  private static final long serialVersionUID = 9120161278939596547L;

  private final String id;
  private final HostAndPort hostAndPort;

  public PID(String id, HostAndPort hostAndPort) {
    this.id = requireNonNull(id);
    this.hostAndPort = requireNonNull(hostAndPort);
  }

  /**
   * Create a PID from a String representation like {@code "scheduler@192.168.1.1:55556"}.
   *
   * @param pid The pid to parse.
   * @return A new PID if the string parsed.
   * @throws IllegalArgumentException If the string failed to parse.
   */
  public static PID fromString(String pid) throws IllegalArgumentException {
    List<String> components = Splitter.on("@").omitEmptyStrings().splitToList(pid);
    if (components.size() != 2) {
      throw new IllegalArgumentException(
          "Illegal libprocess pid: " + pid + ". Format is id@host:port.");
    }
    String id = components.get(0);
    if (CharMatcher.WHITESPACE.matchesAnyOf(id)) {
      throw new IllegalArgumentException("No whitespace allowed in process id.");
    }
    HostAndPort hostAndPort = HostAndPort.fromString(components.get(1))
        .requireBracketsForIPv6();
    if (!hostAndPort.hasPort()) {
      throw new IllegalArgumentException("No port specified.");
    }
    return new PID(id, hostAndPort);
  }

  /**
   * The process's ID.
   *
   * @return The process's ID.
   */
  public String getId() {
    return id;
  }

  /**
   * The host and port that the process is listening on.
   *
   * @return The host and port that the process is listening on.
   */
  public HostAndPort getHostAndPort() {
    return hostAndPort;
  }

  /**
   * The base URL that can be used to send a message to the process running at this PID.
   *
   * @return The base URL.
   */
  public URL getBaseUrl() {
    try {
      return new URL(String.format("http://%s/%s", getHostAndPort(), getId()));
    } catch (MalformedURLException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Get a parsable representation of the process in the same form that's supplied to
   * {@link #fromString(String)}.
   *
   * @return A parsable representation of the PID.
   */
  @Override
  public String toString() {
    return id + "@" + hostAndPort;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PID)) {
      return false;
    }
    PID that = (PID) o;
    return Objects.equals(id, that.id)
        && Objects.equals(hostAndPort, that.hostAndPort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, hostAndPort);
  }
}
