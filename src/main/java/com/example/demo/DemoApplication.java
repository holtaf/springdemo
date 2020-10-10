package com.example.demo;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import javax.swing.*;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@SpringBootApplication
public class DemoApplication {
	public static void main(String[] args) {
//		Flux<Integer> f = Flux.range(0, 20);
//		f.subscribe(i -> System.out.println("First subscriber (" + i + ")"));
//		f.subscribe(i -> System.out.println("Second subscriber (" + i + ")"));

//		Flux.generate(() -> 0, new BiFunction<Integer, SynchronousSink<Integer>, Integer>() {
//			@Override
//			public Integer apply(Integer integer, SynchronousSink<Integer> synchronousSink) {
//				if (integer == 10) {
//					synchronousSink.complete();
//				} else {
//					synchronousSink.next(integer + 1);
//				}
//				return integer + 1;
//			}
//		}, i -> System.out.println("First subscriber (" + i + ")")).subscribe(System.out::println);


//		Flux<Integer> data = Flux.create(new Consumer<FluxSink<Integer>>() {
//			@Override
//			public void accept(FluxSink<Integer> integerFluxSink) {
//				Flux<Integer> d = getData();
//				Disposable disposable = d.subscribe(new Consumer<Integer>() {
//					@Override
//					public void accept(Integer integer) {
//
//					}
//				});
//			}
//		});

//		Flux.range(0, 10000).publishOn(Schedulers.newParallel("asd", 4)).map(i -> Thread.currentThread().getName() + " - " + i).subscribe(System.out::println);

		Flux<Integer> d = getData();
		Disposable disposable = d.subscribe(new Consumer<Integer>() {
			@Override
			public void accept(Integer integer) {

			}
		});


		disposable.dispose();


		Flux<Integer> ff = Flux.range(0, 100).publishOn(Schedulers.parallel()).doOnCancel(new Runnable() {
			@Override
			public void run() {
				System.out.println("Cancelled");
			}
		}).concatWith(d);
		subscription = ff.subscribe(new Consumer<Integer>() {
			@Override
			public void accept(Integer integer) {
				System.out.println(integer);

				if (integer > 20) {
					if (subscription != null) {
						subscription.dispose();
					}
				}
 			}
		});

//		Disposable disposable = d.subscribe();
//		disposable.dispose();

//		Flux.interval(Duration.ofSeconds(1), Schedulers.elastic()).subscribe(System.out::println);

		SpringApplication.run(DemoApplication.class);
//		while (true);
	}

	static Disposable subscription;

	static Flux<Integer> getData() {
		return Flux.interval(Duration.ofSeconds(1)).map(Long::intValue).doOnCancel(new Runnable() {
			@Override
			public void run() {
				System.out.println("Cancelled getData");
			}
		}).doOnSubscribe(new Consumer<Subscription>() {
			@Override
			public void accept(Subscription subscription) {
//				DemoApplication.subscription = subscription;
			}
		}).doOnComplete(new Runnable() {
			@Override
			public void run() {
				System.out.println("Completed getData");
			}
		});
	}

	static class TestSubscriber extends BaseSubscriber<Integer> {
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			super.hookOnSubscribe(subscription);


		}
	}
}
