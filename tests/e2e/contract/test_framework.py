"""
Lightweight, self-contained test assertion and execution framework for E2E suites.
Provides rigorous assertion tracking, timing, tiered categorization, and reporting.
"""
import sys
import time
import traceback
from typing import Callable, List, Dict, Any, Optional


class AssertionFailure(Exception):
    """Raised when a test assertion fails."""
    pass


class TestContext:
    def __init__(self, name: str, tier: str, description: str):
        self.name = name
        self.tier = tier
        self.description = description
        self.assertions_count = 0
        self.errors: List[str] = []
        self.duration_seconds = 0.0
        self.passed = False

    def assert_true(self, condition: bool, message: str = "") -> None:
        self.assertions_count += 1
        if not condition:
            msg = message or "Expected condition to be True, got False"
            raise AssertionFailure(msg)

    def assert_false(self, condition: bool, message: str = "") -> None:
        self.assertions_count += 1
        if condition:
            msg = message or "Expected condition to be False, got True"
            raise AssertionFailure(msg)

    def assert_equal(self, actual: Any, expected: Any, message: str = "") -> None:
        self.assertions_count += 1
        if actual != expected:
            msg = message or f"Expected {expected!r}, but got {actual!r}"
            raise AssertionFailure(msg)

    def assert_not_equal(self, actual: Any, expected: Any, message: str = "") -> None:
        self.assertions_count += 1
        if actual == expected:
            msg = message or f"Expected values to be different, but both are {actual!r}"
            raise AssertionFailure(msg)

    def assert_almost_equal(self, actual: float, expected: float, delta: float = 1e-5, message: str = "") -> None:
        self.assertions_count += 1
        if abs(actual - expected) > delta:
            msg = message or f"Expected {expected} ± {delta}, but got {actual} (diff: {abs(actual - expected)})"
            raise AssertionFailure(msg)

    def assert_in(self, item: Any, container: Any, message: str = "") -> None:
        self.assertions_count += 1
        if item not in container:
            msg = message or f"Expected {item!r} to be in {container!r}"
            raise AssertionFailure(msg)

    def assert_not_in(self, item: Any, container: Any, message: str = "") -> None:
        self.assertions_count += 1
        if item in container:
            msg = message or f"Expected {item!r} not to be in {container!r}"
            raise AssertionFailure(msg)

    def assert_raises(self, exception_cls: type, callable_obj: Callable, *args, **kwargs) -> Any:
        self.assertions_count += 1
        try:
            callable_obj(*args, **kwargs)
        except exception_cls as e:
            return e
        except Exception as e:
            raise AssertionFailure(f"Expected exception {exception_cls.__name__}, but caught {type(e).__name__}: {e}")
        raise AssertionFailure(f"Expected exception {exception_cls.__name__}, but no exception was raised")


class TestCase:
    def __init__(self, name: str, fn: Callable[[TestContext], None], tier: str, description: str):
        self.name = name
        self.fn = fn
        self.tier = tier
        self.description = description

    def run(self) -> TestContext:
        ctx = TestContext(self.name, self.tier, self.description)
        start_time = time.perf_counter()
        try:
            self.fn(ctx)
            ctx.passed = True
        except AssertionFailure as af:
            ctx.passed = False
            ctx.errors.append(f"Assertion failed: {af}")
            ctx.errors.append(traceback.format_exc())
        except Exception as ex:
            ctx.passed = False
            ctx.errors.append(f"Unexpected error: {type(ex).__name__}: {ex}")
            ctx.errors.append(traceback.format_exc())
        finally:
            ctx.duration_seconds = time.perf_counter() - start_time
        return ctx


class TestRegistry:
    def __init__(self):
        self.test_cases: List[TestCase] = []

    def register(self, tier: str, description: str = "") -> Callable[[Callable[[TestContext], None]], Callable]:
        def decorator(fn: Callable[[TestContext], None]) -> Callable:
            tc = TestCase(name=fn.__name__, fn=fn, tier=tier, description=description or fn.__doc__ or "")
            self.test_cases.append(tc)
            return fn
        return decorator

    def get_by_tier(self, tier: str) -> List[TestCase]:
        return [tc for tc in self.test_cases if tc.tier.lower() == tier.lower()]


registry = TestRegistry()
test = registry.register
