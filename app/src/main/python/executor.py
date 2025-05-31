import sys
from io import StringIO

def run_code(code):
    try:
        # Çıxışı tutmaq üçün buffer
        old_stdout = sys.stdout
        redirected_output = sys.stdout = StringIO()

        # Kodu icra et
        exec(code)

        # Çıxışı əldə et
        output = redirected_output.getvalue()
        sys.stdout = old_stdout

        return output if output else "Kod icra edildi (çıxış yoxdur)"
    except Exception as e:
        return f"Xəta: {str(e)}"