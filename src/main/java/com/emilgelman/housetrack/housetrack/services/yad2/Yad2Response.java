package com.emilgelman.housetrack.housetrack.services.yad2;

import com.emilgelman.housetrack.housetrack.domain.SellerType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@JsonDeserialize(using = Yad2Response.Deserializer.class)
@RequiredArgsConstructor
public class Yad2Response {
    @Getter
    private final Set<FeedItem> items;

    @Getter
    @Builder
    public static class FeedItem {
        private String id;
        private Long price;
        private Long rooms;
        private Long squareMeters;
        private Long floor;
        private String city;
        private String neighborhood;
        private String street;
        private SellerType sellerType;
        private LocalDate dateAdded;

    }

    protected static class Deserializer extends StdDeserializer<Yad2Response> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Yad2Response deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            JsonNode jsonNode = node.get("feed").get("feed_items");

            Set<FeedItem> feedItems = StreamSupport.stream(jsonNode.spliterator(), false)
                    .filter(x -> x.get("type").asText().equals("ad"))
                    .map(this::createFeedItem)
                    .collect(Collectors.toSet());
            return new Yad2Response(feedItems);
        }

        private FeedItem createFeedItem(JsonNode x) {
            return FeedItem.builder()
                    .id(x.get("id").asText())
                    .city(x.get("city").asText())
                    .neighborhood(x.get("neighborhood").asText())
                    .street(parseStreet(x))
                    .rooms(x.get("Rooms_text").asLong())
                    .price(parsePrice(x))
                    .floor(parseFloor(x))
                    .squareMeters(x.get("square_meters").asLong())
                    .sellerType(SellerType.from(x.get("merchant").asBoolean()))
                    .dateAdded(LocalDateTime.parse(x.get("date_added").asText().replace(" ","T")).toLocalDate())
                    .build();
        }

        private String parseStreet(JsonNode x) {
            return Optional.ofNullable(x.get("street"))
                    .map(JsonNode::asText)
                    .orElse(Strings.EMPTY);
        }

        private Long parsePrice(JsonNode x) {
            String price = x.get("price").asText();
            try {
                return Long.valueOf(price.replaceAll("[^\\d.]", ""));
            }
            catch (NumberFormatException ex)
            {
                log.warn("Caught exception trying to format price {}", price);
                return 0L;
            }
        }

        private Long parseFloor(JsonNode x) {
            return Optional.ofNullable(x.get("row_4"))
                    .map(n -> n.get(1))
                    .map(n -> n.get("value").asLong())
                    .orElse(0L);
        }
    }
}
