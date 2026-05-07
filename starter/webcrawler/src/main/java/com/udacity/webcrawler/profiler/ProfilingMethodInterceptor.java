package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
private final ProfilingState state;
private final Object delegate;
 
  ProfilingMethodInterceptor(
        Clock clock,
        ProfilingState state,
        Object delegate) {

    this.clock = Objects.requireNonNull(clock);
    this.state = Objects.requireNonNull(state);
    this.delegate = Objects.requireNonNull(delegate);
}

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable{
    
if (method.getDeclaringClass().equals(Object.class)) {
    return method.invoke(delegate, args);
}
if (!method.isAnnotationPresent(Profiled.class)) {
    return method.invoke(delegate, args);
}
Instant start = clock.instant();
    try {

    return method.invoke(delegate, args);

} catch (InvocationTargetException e) {

    throw e.getCause();

} finally {

    Instant end = clock.instant();

    state.record(
            delegate.getClass(),
            method,
            Duration.between(start, end));
}
  }
}
