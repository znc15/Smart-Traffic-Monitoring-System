package com.smarttraffic.backend.service.analytics;

import com.smarttraffic.backend.dto.traffic.PredictionPointResponse;
import com.smarttraffic.backend.dto.traffic.PredictionResponse;
import com.smarttraffic.backend.model.TrafficPredictionEntity;
import com.smarttraffic.backend.model.TrafficSampleEntity;
import com.smarttraffic.backend.repository.TrafficPredictionRepository;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrafficPredictionService {

    public static final String DEFAULT_ALGORITHM = "seasonal-weighted-average-v1";

    private final TrafficSampleRepository sampleRepository;
    private final TrafficPredictionRepository predictionRepository;

    public TrafficPredictionService(TrafficSampleRepository sampleRepository, TrafficPredictionRepository predictionRepository) {
        this.sampleRepository = sampleRepository;
        this.predictionRepository = predictionRepository;
    }

    @Transactional
    public PredictionResponse generatePrediction(String roadName, int horizonMinutes) {
        int horizon = Math.max(1, Math.min(180, horizonMinutes));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(7);

        List<TrafficSampleEntity> history = sampleRepository.findByRoadNameAndSampleTimeBetweenOrderBySampleTimeAsc(
                roadName,
                from,
                now
        );

        double baseFlow = calculateBaseFlow(history);
        double trendSlope = calculateTrendSlope(history);

        List<PredictionPointResponse> points = new ArrayList<>();
        List<TrafficPredictionEntity> predictionEntities = new ArrayList<>();

        for (int i = 1; i <= horizon; i++) {
            LocalDateTime ts = now.plusMinutes(i);
            double predictedFlow = Math.max(0.0, baseFlow + trendSlope * i);
            double ci = Math.max(2.0, predictedFlow * 0.12);
            double low = Math.max(0.0, predictedFlow - ci);
            double high = predictedFlow + ci;

            predictedFlow = round3(predictedFlow);
            low = round3(low);
            high = round3(high);

            points.add(new PredictionPointResponse(ts, predictedFlow, low, high));

            TrafficPredictionEntity entity = new TrafficPredictionEntity();
            entity.setRoadName(roadName);
            entity.setGeneratedAt(now);
            entity.setPredictTime(ts);
            entity.setPredictedFlow(predictedFlow);
            entity.setConfidenceLow(low);
            entity.setConfidenceHigh(high);
            entity.setAlgorithm(DEFAULT_ALGORITHM);
            predictionEntities.add(entity);
        }

        predictionRepository.saveAll(predictionEntities);
        return new PredictionResponse(roadName, now, DEFAULT_ALGORITHM, points);
    }

    private static double calculateBaseFlow(List<TrafficSampleEntity> history) {
        if (history == null || history.isEmpty()) {
            return 0.0;
        }

        double weightedSum = 0.0;
        double weightTotal = 0.0;
        int n = history.size();
        for (int i = 0; i < n; i++) {
            TrafficSampleEntity sample = history.get(i);
            double flow = sample.getCountCar() + sample.getCountMotor() + sample.getCountPerson();
            double weight = 1.0 + (double) i / n;
            weightedSum += flow * weight;
            weightTotal += weight;
        }

        return weightTotal == 0.0 ? 0.0 : weightedSum / weightTotal;
    }

    private static double calculateTrendSlope(List<TrafficSampleEntity> history) {
        if (history == null || history.size() < 2) {
            return 0.0;
        }

        int tailSize = Math.min(12, history.size());
        List<TrafficSampleEntity> tail = history.subList(history.size() - tailSize, history.size());
        double first = tail.get(0).getCountCar() + tail.get(0).getCountMotor() + tail.get(0).getCountPerson();
        double last = tail.get(tail.size() - 1).getCountCar()
                + tail.get(tail.size() - 1).getCountMotor()
                + tail.get(tail.size() - 1).getCountPerson();

        return (last - first) / Math.max(1.0, tail.size() - 1.0);
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
