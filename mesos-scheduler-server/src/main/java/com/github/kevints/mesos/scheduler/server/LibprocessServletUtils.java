package com.github.kevints.mesos.scheduler.server;/*
 * Copyright 2013 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.file.Paths;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.github.kevints.libprocess.client.PID;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;

public final class LibprocessServletUtils {
  private LibprocessServletUtils() {
    // Utility class.
  }

  static Optional<String> parseMessageType(HttpServletRequest req) {
    String pathInfo = req.getPathInfo();
    if (pathInfo == null) {
      return Optional.absent();
    }

    return Optional.of(Paths.get(pathInfo).getFileName().toString());
  }

  static Optional<PID> getSenderPid(HttpServletRequest req) {
    Optional<String> libprocessFrom = Optional.fromNullable(req.getHeader("X-Libprocess-From"));
    if (libprocessFrom.isPresent()) {
      try {
        return Optional.of(PID.fromString(libprocessFrom.get()));
      } catch (IllegalArgumentException e) {
        return Optional.absent();
      }
    }

    Optional<String> userAgent = Optional.fromNullable(req.getHeader("User-Agent"));
    if (userAgent.isPresent()) {
      List<String> pid = Splitter.on("libprocess/").omitEmptyStrings().splitToList(userAgent.get());
      if (pid.size() != 1) {
        return Optional.absent();
      } else {
        try {
          return Optional.of(PID.fromString(pid.get(0)));
        } catch (IllegalArgumentException e) {
          return Optional.absent();
        }
      }
    }

    return Optional.absent();
  }
}
