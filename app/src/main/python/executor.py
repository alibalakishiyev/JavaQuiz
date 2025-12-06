import sys
import io
import math
import random
from datetime import datetime
import json
import re

def run_code(code, user_input=None):
    try:
        # Output'u yönləndir
        old_stdout = sys.stdout
        old_stderr = sys.stderr
        output_buffer = io.StringIO()
        sys.stdout = output_buffer
        sys.stderr = output_buffer

        # BÜTÜN KİTABXANALARI GLOBAL NAMESPACE-Ə ƏLAVƏ ET
        global_namespace = {
            '__builtins__': __builtins__,
            'math': math,
            'random': random,
            'datetime': datetime,
            'json': json,
            're': re
        }

        # KİTABXANALARI IMPORT ET
        try:
            import numpy as np
            global_namespace['np'] = np
            global_namespace['numpy'] = np
        except ImportError:
            pass

        try:
            import torch
            global_namespace['torch'] = torch
        except ImportError:
            pass

        try:
            import matplotlib.pyplot as plt
            global_namespace['plt'] = plt
            global_namespace['matplotlib'] = plt
        except ImportError:
            pass

        try:
            from PIL import Image
            global_namespace['Image'] = Image
            global_namespace['PIL'] = Image
        except ImportError:
            pass

        try:
            import pandas as pd
            global_namespace['pd'] = pd
            global_namespace['pandas'] = pd
        except ImportError:
            pass

        try:
            import scipy
            global_namespace['scipy'] = scipy
        except ImportError:
            pass

        # Əgər input varsa, onu emulyasiya et
        if user_input:
            def custom_input(prompt=""):
                print(prompt, end='')
                return user_input
            global_namespace['input'] = custom_input

        # Kodu icra et
        exec(code, global_namespace)

        # Output'u geri qaytar
        sys.stdout = old_stdout
        sys.stderr = old_stderr

        result = output_buffer.getvalue()
        return result if result else "> Kod uğurla icra olundu"

    except Exception as e:
        # Error halında output'u geri qaytar
        if 'old_stdout' in locals():
            sys.stdout = old_stdout
            sys.stderr = old_stderr
        return f"Xəta: {str(e)}"