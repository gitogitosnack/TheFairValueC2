import logging

import yfinance as yf
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

logger = logging.getLogger(__name__)

router = APIRouter()

JP_TICKER_SUFFIX = ".T"


class QuoteResponse(BaseModel):
    symbol: str
    date: str
    open: float | None
    high: float | None
    low: float | None
    close: float | None
    volume: int | None
    market_cap: int | None
    shares_outstanding: int | None


def to_yahoo_symbol(symbol: str) -> str:
    """日本株の4桁証券コードに `.T` サフィックスを付与する。

    既にサフィックス（`.` を含む）が付いている場合はそのまま扱う。
    """
    if "." in symbol:
        return symbol
    return f"{symbol}{JP_TICKER_SUFFIX}"


@router.get("/quotes/{symbol}", response_model=QuoteResponse)
def get_quote(symbol: str) -> QuoteResponse:
    yahoo_symbol = to_yahoo_symbol(symbol)
    ticker = yf.Ticker(yahoo_symbol)

    history = ticker.history(period="5d")
    if history.empty:
        raise HTTPException(
            status_code=404,
            detail=f"quote data not found for symbol: {yahoo_symbol}",
        )

    latest = history.iloc[-1]
    latest_date = history.index[-1].strftime("%Y-%m-%d")

    market_cap: int | None = None
    shares_outstanding: int | None = None
    try:
        fast_info = ticker.fast_info
        market_cap = fast_info.get("market_cap") or fast_info.get("marketCap")
        shares_outstanding = fast_info.get("shares") or fast_info.get(
            "sharesOutstanding"
        )
    except Exception:
        logger.warning("failed to read fast_info for symbol: %s", yahoo_symbol, exc_info=True)

    return QuoteResponse(
        symbol=yahoo_symbol,
        date=latest_date,
        open=float(latest["Open"]) if "Open" in latest else None,
        high=float(latest["High"]) if "High" in latest else None,
        low=float(latest["Low"]) if "Low" in latest else None,
        close=float(latest["Close"]) if "Close" in latest else None,
        volume=int(latest["Volume"]) if "Volume" in latest else None,
        market_cap=int(market_cap) if market_cap is not None else None,
        shares_outstanding=(
            int(shares_outstanding) if shares_outstanding is not None else None
        ),
    )
