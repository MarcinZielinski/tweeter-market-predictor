package pl.suu.predictor.stock.model

case class StockData(
                      symbols_requested: Int,
                      symbols_returned: Int,
                      data: List[StockDetails]
                    )

case class StockDetails(
                         symbol: String,
                         name: String,
                         currency: String,
                         price: String,
                         price_open: String,
                         day_high: String,
                         day_low: String,
                         // 52_week_high: String, json starts with number.. omit these two fields
                         // 52_week_low: String,
                         day_change: String,
                         change_pct: String,
                         close_yesterday: String,
                         market_cap: String,
                         volume: String,
                         volume_avg: String,
                         shares: String,
                         stock_exchange_long: String,
                         stock_exchange_short: String,
                         timezone: String,
                         timezone_name: String,
                         gmt_offset: String,
                         last_trade_time: String
                       )
