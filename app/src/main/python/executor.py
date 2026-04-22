import sys
import io
import math, random, json, re
from datetime import datetime
import time
from threading import Event

_input_queue = None
_prompt_queue = None
_stop_event = Event()  # STOP siqnalı üçün

def set_input_queue(queue):
    global _input_queue
    _input_queue = queue

def set_prompt_queue(queue):
    global _prompt_queue
    _prompt_queue = queue

def stop_execution():
    """İcra edən thread-i dayandır"""
    _stop_event.set()

def reset_stop():
    """Stop siqnalını sıfırla"""
    _stop_event.clear()

def run_code(code, stdin_data=""):
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    old_stdin  = sys.stdin

    output_buffer = io.StringIO()
    sys.stdout = output_buffer
    sys.stderr = output_buffer

    global_namespace = {
        '__builtins__': __builtins__,
        'math': math, 'random': random,
        'datetime': datetime, 'json': json, 're': re,
    }

    for mod, alias in [('numpy','np'),('pandas','pd'),('scipy','scipy')]:
        try:
            m = __import__(mod)
            global_namespace[alias] = m
            global_namespace[mod] = m
        except ImportError:
            pass

    try:
        import matplotlib.pyplot as plt
        global_namespace['plt'] = plt
    except ImportError:
        pass

    def dynamic_input(prompt=""):
        """Timeout və stop ilə input funksiyası"""
        if prompt:
            sys.stdout.write(str(prompt))
            sys.stdout.flush()

        # Prompt-u Java-ya göndər
        if _prompt_queue is not None:
            try:
                _prompt_queue.put(str(prompt) if str(prompt).strip() else " ")
            except:
                pass

        # Cavabı timeout ilə gözlə (30 saniyə)
        if _input_queue is not None:
            try:
                # poll(timeout, unit) - timeout ilə oxu
                # SynchronousQueue üçün poll() istifadə et
                val = _input_queue.poll(30, java.util.concurrent.TimeUnit.SECONDS)

                # Stop siqnalını yoxla
                if _stop_event.is_set():
                    return ""

                if val is None:
                    # Timeout baş verdi
                    raise TimeoutError("Input timeout (30 seconds)")

                return str(val)
            except Exception as e:
                if "Timeout" in str(e) or isinstance(e, TimeoutError):
                    raise RuntimeError("⏰ Input timeout! İstifadəçi 30 saniyə ərzində cavab vermədi.")
                raise RuntimeError(f"Input error: {e}")

        return ""

    global_namespace['input'] = dynamic_input
    reset_stop()  # Stop siqnalını sıfırla

    try:
        exec(compile(code, '<string>', 'exec'), global_namespace)
    except TimeoutError as e:
        output_buffer.write(f"\n⏰ {str(e)}")
    except Exception as e:
        output_buffer.write(f"\n❌ Xəta: {type(e).__name__}: {str(e)}")
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr
        sys.stdin  = old_stdin

    result = output_buffer.getvalue()
    return result if result.strip() else "> Kod uğurla icra olundu"