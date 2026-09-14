from app import llm


def test_progress_fallback_without_model(monkeypatch):
    monkeypatch.setattr(llm, "_completion", lambda *_: None)
    assert llm.decide("我的计划完成多少了", [])[0] == "PROGRESS"


def test_course_search_fallback_without_model(monkeypatch):
    monkeypatch.setattr(llm, "_completion", lambda *_: None)
    assert llm.decide("推荐 Java 微服务课程", [])[0] == "COURSE_SEARCH"


def test_invalid_model_tool_is_rejected(monkeypatch):
    monkeypatch.setattr(llm, "_completion", lambda *_: '{"intent":"PAY_ORDER","reply":"已支付"}')
    intent, _, model_used = llm.decide("帮我支付", [])
    assert intent == "GENERAL"
    assert model_used is True
