/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.examples.windowing;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.delta.DeltaFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.evictors.TimeEvictor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.DeltaTrigger;

import java.time.Duration;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * An example of grouped stream windowing where different eviction and trigger policies can be used.
 * A source fetches events from cars every 100 msec containing their id, their current speed (kmh),
 * overall elapsed distance (m) and a timestamp. The streaming example triggers the top speed of
 * each car every x meters elapsed for the last y seconds.
 */
public class TopSpeedWindowing {

    // *************************************************************************
    // PROGRAM
    // *************************************************************************

    public static void main(String[] args) throws Exception {

        final ParameterTool params = ParameterTool.fromArgs(args);

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(params);

        @SuppressWarnings({"rawtypes", "serial"})
        DataStream<Tuple4<Integer, Integer, Double, Long>> carData;
        if (params.has("input")) {
            carData = env.readTextFile(params.get("input")).map(new ParseCarData());
        } else {
            System.out.println("Executing TopSpeedWindowing example with default input data set.");
            System.out.println("Use --input to specify file input.");
            carData = env.addSource(CarSource.create(2));
        }
        WatermarkStrategy<Tuple4<Integer, Integer, Double, Long>> watermarkStrategy =
                WatermarkStrategy.<Tuple4<Integer, Integer, Double, Long>>forBoundedOutOfOrderness(
                                Duration.ofSeconds(2))
                        .withTimestampAssigner(
                                new SerializableTimestampAssigner<
                                        Tuple4<Integer, Integer, Double, Long>>() {
                                    @Override
                                    public long extractTimestamp(
                                            Tuple4<Integer, Integer, Double, Long> element,
                                            long recordTimestamp) {
                                        return element.f3;
                                    }
                                });

        int evictionSec = 10;
        double triggerMeters = 50;
        DataStream<Tuple4<Integer, Integer, Double, Long>> topSpeeds =
                // carData.assignTimestampsAndWatermarks(new CarTimestamp())
                carData.assignTimestampsAndWatermarks(watermarkStrategy)
                        .keyBy(value -> value.f0)
                        .window(TumblingEventTimeWindows.of(Time.seconds(3)))
                        .evictor(TimeEvictor.of(Time.of(evictionSec, TimeUnit.SECONDS)))
                        .trigger(
                                DeltaTrigger.of(
                                        triggerMeters,
                                        new DeltaFunction<
                                                Tuple4<Integer, Integer, Double, Long>>() {
                                            private static final long serialVersionUID = 1L;

                                            @Override
                                            public double getDelta(
                                                    Tuple4<Integer, Integer, Double, Long>
                                                            oldDataPoint,
                                                    Tuple4<Integer, Integer, Double, Long>
                                                            newDataPoint) {
                                                return newDataPoint.f2 - oldDataPoint.f2;
                                            }
                                        },
                                        carData.getType().createSerializer(env.getConfig())))
                        .maxBy(1);

        if (params.has("output")) {
            topSpeeds.writeAsText(params.get("output"));
        } else {
            System.out.println("Printing result to stdout. Use --output to specify output path.");
            topSpeeds.print();
        }

        env.execute("CarTopSpeedWindowingExample");
    }

    // *************************************************************************
    // USER FUNCTIONS
    // *************************************************************************

    private static class CarSource
            implements SourceFunction<Tuple4<Integer, Integer, Double, Long>> {

        private static final long serialVersionUID = 1L;
        private Integer[] speeds;
        private Double[] distances;

        private Random rand = new Random();

        private volatile boolean isRunning = true;

        private CarSource(int numOfCars) {
            speeds = new Integer[numOfCars];
            distances = new Double[numOfCars];
            Arrays.fill(speeds, 50);
            Arrays.fill(distances, 0d);
        }

        public static CarSource create(int cars) {
            return new CarSource(cars);
        }

        @Override
        public void run(SourceContext<Tuple4<Integer, Integer, Double, Long>> ctx)
                throws Exception {

            while (isRunning) {
                Thread.sleep(100);
                for (int carId = 0; carId < speeds.length; carId++) {
                    if (rand.nextBoolean()) {
                        speeds[carId] = Math.min(100, speeds[carId] + 5);
                    } else {
                        speeds[carId] = Math.max(0, speeds[carId] - 5);
                    }
                    distances[carId] += speeds[carId] / 3.6d;
                    Tuple4<Integer, Integer, Double, Long> record =
                            new Tuple4<>(
                                    carId,
                                    speeds[carId],
                                    distances[carId],
                                    System.currentTimeMillis());
                    ctx.collect(record);
                }
            }
        }

        @Override
        public void cancel() {
            isRunning = false;
        }
    }

    private static class ParseCarData
            extends RichMapFunction<String, Tuple4<Integer, Integer, Double, Long>> {
        private static final long serialVersionUID = 1L;

        @Override
        public Tuple4<Integer, Integer, Double, Long> map(String record) {
            String rawData = record.substring(1, record.length() - 1);
            String[] data = rawData.split(",");
            return new Tuple4<>(
                    Integer.valueOf(data[0]),
                    Integer.valueOf(data[1]),
                    Double.valueOf(data[2]),
                    Long.valueOf(data[3]));
        }
    }

    private static class CarTimestamp
            extends AscendingTimestampExtractor<Tuple4<Integer, Integer, Double, Long>> {
        private static final long serialVersionUID = 1L;

        @Override
        public long extractAscendingTimestamp(Tuple4<Integer, Integer, Double, Long> element) {
            return element.f3;
        }
    }
}
