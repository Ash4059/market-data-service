package com.MarketPulse.Market_data_service.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstrumentData {

    @JsonProperty("instrument_key")
    private String instrumentKey;

    @JsonProperty("exchange_token")
    private String exchangeToken;

    @JsonProperty("trading_symbol")
    private String tradingSymbol;

    @JsonProperty("name")
    private String name;

    @JsonProperty("short_name")
    private String shortName;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("segment")
    private String segment;

    @JsonProperty("instrument_type")
    private String instrumentType;

    @JsonProperty("isin")
    private String isin;

    @JsonProperty("lot_size")
    private Integer lotSize;

    @JsonProperty("tick_size")
    private Double tickSize;

    @JsonProperty("freeze_quantity")
    private Double freezeQuantity;

    @JsonProperty("underlying_key")
    private String underlyingKey;

    @JsonProperty("underlying_type")
    private String underlyingType;

    @JsonProperty("option_type")
    private String optionType;

    @JsonProperty("strike_price")
    private Double strikePrice;

    @JsonProperty("expiry")
    private String expiry;

    @JsonProperty("weekly")
    private Boolean weekly;

    @JsonProperty("minimum_lot_size")
    private Integer minimumLotSize;

    @JsonProperty("precision")
    private Integer precision;

    @JsonProperty("multiplier")
    private Double multiplier;

    @JsonProperty("base_price")
    private Double basePrice;

    @JsonProperty("cross_currency")
    private String crossCurrency;

    @JsonProperty("underlying_symbol")
    private String underlyingSymbol;

    @JsonProperty("underlying_listing_id")
    private String underlyingListingId;

    @JsonProperty("underlying_group")
    private String underlyingGroup;

    @JsonProperty("market_lot")
    private Integer marketLot;

    @JsonProperty("is_index")
    private Boolean isIndex;

    @JsonProperty("max_single_order_quantity")
    private Long maxSingleOrderQuantity;

    @JsonProperty("max_single_order_value")
    private Double maxSingleOrderValue;

    @JsonProperty("max_quantity_freeze")
    private Long maxQuantityFreeze;

    @JsonProperty("daily_price_range_lower")
    private Double dailyPriceRangeLower;

    @JsonProperty("daily_price_range_upper")
    private Double dailyPriceRangeUpper;

}
