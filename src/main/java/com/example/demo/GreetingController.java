package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@RestController
public class GreetingController {
	private final ManagableThreadPoolExecutor threadPoolExecutor;

	public GreetingController() {
		threadPoolExecutor = new ManagableThreadPoolExecutor();
	}

	@GetMapping(value = "foo")
	public Flux<ServerSentEvent<Object>> foo() {
		return wrapResponse(UUID.randomUUID().toString(), this::startGeneratingValues);
	}

	@GetMapping(value = "tasks/cancel/{id}")
	public void cancelTask(@PathVariable("id") String id) {
		threadPoolExecutor.cancel(id);
	}

	@GetMapping(value = "tasks/pause/{id}")
	public void pauseTask(@PathVariable("id") String id) {
		threadPoolExecutor.pause(id);
	}

	@GetMapping(value = "tasks/resume/{id}")
	public void resumeTask(@PathVariable("id") String id) {
		threadPoolExecutor.resume(id);
	}

	@GetMapping(value = "tasks/get")
	public Set<String> getTasks() {
		return threadPoolExecutor.getTaskSet();
	}

	private Flux<ServerSentEvent<Object>> wrapResponse(String taskId, Consumer<FluxSink<Event>> sinkConsumer) {
		Mono<Event> start = Mono.just(new StartEvent(taskId));

		System.out.println("Thread name: " + Thread.currentThread().getName());
		Flux<Event> events = Flux.create(eventFluxSink -> {
			threadPoolExecutor.submitTask(taskId, () -> {
				try {
					sinkConsumer.accept(eventFluxSink);
				} catch (Throwable e) {
					eventFluxSink.next(new ExceptionEvent(e));
					eventFluxSink.complete();
				}
			});
		});

//		Flux<Event> events =  Flux.create(sinkConsumer);

		return Flux.concat(start, events).map(o -> ServerSentEvent.builder().data(o.getData()).event(o.getType()).build());
	}

	private void startGeneratingValues(FluxSink<Event> sink) {
		System.out.println("Thread name: " + Thread.currentThread().getName());

		final Random random = new Random();

//		final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//		service.scheduleAtFixedRate(new Runnable() {

		File file = new File("test.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		sink.next(new DataEvent(new Graph(0, String.valueOf(random.nextFloat()))));
		for (int i = 0; i < 20000; i++) {
			sink.next(new ProgressEvent(i * 5));


			if (fileWriter != null) {
				try {
					fileWriter.append('c');

					if (i % 5 == 0) {
						fileWriter.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();

//				throw new RuntimeException(e);
			}
		}

		sink.next(new ProgressEvent(100));
		sink.complete();


//			@Override
//			public void run() {
//		}, 300, 300, TimeUnit.MILLISECONDS);
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
		Object getData();
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