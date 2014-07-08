/**
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
package com.github.kevints.mesoss;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import static java.util.Objects.requireNonNull;

public class ProtoMessageServlet<T extends Message> extends HttpServlet {
  private final Parser<T> parser;
  private final ProtoMessageHandler<T> handler;

  ProtoMessageServlet(Parser<T> parser, ProtoMessageHandler<T> handler) {
    this.parser = requireNonNull(parser);
    this.handler = requireNonNull(handler);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    T message = parser.parseFrom(req.getInputStream());
    try {
      handler.handle(message);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
}
