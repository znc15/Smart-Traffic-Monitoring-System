from pathlib import Path
import sys

import pytest

sys.path.append(str(Path(__file__).resolve().parents[1]))

import main
from state import state


class _DummyThread:
    def __init__(self, alive: bool = True, stop_on_join: bool = False):
        self._alive = alive
        self._stop_on_join = stop_on_join
        self.join_calls: list[float | None] = []

    def is_alive(self) -> bool:
        return self._alive

    def join(self, timeout: float | None = None) -> None:
        self.join_calls.append(timeout)
        if self._stop_on_join:
            self._alive = False


def setup_function():
    state.reset_stop()
    state.set_loop_state("stopped")


def test_restart_loop_does_not_start_new_thread_when_old_thread_times_out(monkeypatch):
    stubborn_thread = _DummyThread(alive=True, stop_on_join=False)
    start_calls: list[bool] = []

    monkeypatch.setattr(main, "_loop_thread", stubborn_thread)
    monkeypatch.setattr(
        main,
        "_start_loop_locked",
        lambda model_changed=False: start_calls.append(model_changed),
    )
    state.set_loop_state("running")

    with pytest.raises(main.LoopRestartError):
        main.restart_loop(model_changed=False)

    assert stubborn_thread.join_calls == [main._RESTART_TIMEOUT_SEC]
    assert start_calls == []
    assert main._loop_thread is stubborn_thread
    assert state.get_loop_state() == "running"
    assert state.should_stop() is False


def test_restart_loop_starts_replacement_only_after_old_thread_stops(monkeypatch):
    old_thread = _DummyThread(alive=True, stop_on_join=True)
    new_thread = _DummyThread(alive=True, stop_on_join=False)
    start_calls: list[bool] = []

    def fake_start_loop_locked(model_changed: bool = False) -> None:
        start_calls.append(model_changed)
        state.reset_stop()
        state.clear_frame()
        state.set_loop_state("starting")
        main._loop_thread = new_thread

    monkeypatch.setattr(main, "_loop_thread", old_thread)
    monkeypatch.setattr(main, "_start_loop_locked", fake_start_loop_locked)
    state.set_loop_state("running")

    main.restart_loop(model_changed=True)

    assert old_thread.join_calls == [main._RESTART_TIMEOUT_SEC]
    assert start_calls == [True]
    assert main._loop_thread is new_thread
    assert state.get_loop_state() == "starting"
    assert state.should_stop() is False
