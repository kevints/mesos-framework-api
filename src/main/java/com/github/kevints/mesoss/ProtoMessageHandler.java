package com.github.kevints.mesoss;

interface ProtoMessageHandler<T> {
  void handle(T message) throws Exception;
}
