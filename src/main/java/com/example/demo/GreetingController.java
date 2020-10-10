package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@RestController
public class GreetingController {
	private final ConcurrentMap<String, Disposable> disposableMap;

	public GreetingController() {
		disposableMap = new ConcurrentHashMap<>();
	}

	@GetMapping(value = "foo")
	public Flux<ServerSentEvent<Object>> foo() {
		return wrapResponse2(UUID.randomUUID().toString(), this::getData1);
	}

	@GetMapping(value = "tasks/cancel/{id}")
	public void cancelTask(@PathVariable("id") String id) {
		Disposable disposable = disposableMap.get(id);
		if (disposable != null) {
			disposable.dispose();
			disposableMap.remove(id);
		}
	}

//	@GetMapping(value = "tasks/pause/{id}")
//	public void pauseTask(@PathVariable("id") String id) {
//		threadPoolExecutor.pause(id);
//	}
//
//	@GetMapping(value = "tasks/resume/{id}")
//	public void resumeTask(@PathVariable("id") String id) {
//		threadPoolExecutor.resume(id);
//	}

	@GetMapping(value = "tasks/get")
	public Set<String> getTasks() {
		return disposableMap.keySet();
	}

	private Flux<ServerSentEvent<Object>> wrapResponse2(String taskId, Supplier<Flux<Event>> method) {
		Mono<Event> start = Mono.just(new StartEvent(taskId));
		Flux<Event> forwardedDataFlux = Flux.create(eventFluxSink -> {
			Flux<Event> dataFlux = method.get();
			Disposable disposable = dataFlux
					.doOnCancel(() -> {
						eventFluxSink.next(new CancelEvent());
						eventFluxSink.complete();

						disposableMap.remove(taskId);
					})
					.doOnComplete(() -> {
						eventFluxSink.next(new CompleteEvent());
						eventFluxSink.complete();

						disposableMap.remove(taskId);
					})
					.doOnError(throwable -> {
						eventFluxSink.next(new ExceptionEvent(throwable));
						eventFluxSink.complete();

						disposableMap.remove(taskId);
					})
					.doOnNext(eventFluxSink::next)
					.subscribe();

			disposableMap.put(taskId, disposable);
		});

		return Flux.concat(start, forwardedDataFlux).map(o -> ServerSentEvent.builder().data(o.getData()).event(o.getType()).build());
	}

	private Flux<Event> getData1() {
		return Flux.range(0, 10)
				.delayElements(Duration.ofSeconds(1))
				.publishOn(Schedulers.boundedElastic())
				.<Event>map(i -> new DataEvent(new Graph(i, "HELLO")))
				.doOnNext(event -> System.out.println("Event " + event.getData() + " on thread " + Thread.currentThread().getName()));
	}

	public static class Graph {
		private long id;
		private String text;

		public Graph() {

		}

		public Graph(long id, String text) {
			this.id = id;
			this.text = text;
		}

		public void setId(long id) {
			this.id = id;
		}

		public void setText(String text) {
			this.text = text;
		}

		public long getId() {
			return id;
		}

		public String getText() {
			return text;
		}
	}

	public interface Event {
		@JsonIgnore
		String getType();

		@JsonIgnore
		default Object getData() {
			return null;
		}
	}

	public static class StartEvent implements Event {
		public String id;

		public StartEvent(String id) {
			this.id = id;
		}

		@Override
		public String getType() {
			return "start";
		}

		@Override
		public Object getData() {
			return id;
		}
	}

	public static class CancelEvent implements Event {
		@Override
		public String getType() {
			return "cancel";
		}

		@Override
		public Object getData() {
			return null;
		}
	}

	public static class CompleteEvent implements Event {
		@Override
		public String getType() {
			return "complete";
		}
	}



	public static class ProgressEvent implements Event {
		public int progress;

		public ProgressEvent(int progress) {
			this.progress = progress;
		}

		@Override
		public String getType() {
			return "progress";
		}

		@Override
		public Object getData() {
			return progress;
		}
	}

	public static class DataEvent implements Event {
		public Graph graph;

		public DataEvent(Graph graph) {
			this.graph = graph;
		}

		@Override
		public String getType() {
			return "data";
		}

		@Override
		public Object getData() {
			return graph;
		}
	}

	public static class ExceptionEvent implements Event {
		public Throwable throwable;

		public ExceptionEvent(Throwable throwable) {
			this.throwable = throwable;
		}

		@Override
		public String getType() {
			return "exception";
		}

		@Override
		public Object getData() {
			return throwable;
		}
	}
}