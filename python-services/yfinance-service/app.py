from fastapi import FastAPI

from routers import quote

app = FastAPI(title="yfinance-service")
app.include_router(quote.router)


@app.get("/health")
def health() -> dict[str, str]:
    """Java側バッチ起動前の疎通確認用エンドポイント（設計書 5.3・7 章参照）。"""
    return {"status": "ok"}
