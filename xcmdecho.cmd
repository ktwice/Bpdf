::
:: Drag file or dir here
::

@echo  - currdir: ^<cd^>%CD%^</cd^>
@echo  - cmd
@echo   percent-0: ^<%%^>%0^</%%^>
@echo  w/o quotes: ^<~^>%~0^</~^>
@echo    fullname: ^<f^>%~f0^</f^>
@echo        disk: ^<d^>%~d0^</d^>
@echo        path: ^<p^>%~p0^</p^>
@echo        name: ^<n^>%~n0^</n^>
@echo         eXt: ^<x^>%~x0^</x^>
@echo       short: ^<s^>%~s0^</s^>
@echo        attr: ^<a^>%~a0^</a^>
@echo        time: ^<t^>%~t0^</t^>
@echo        siZe: ^<z^>%~z0^</z^>
@if %1a==a goto no_arg1_label
@echo  - arg1
@echo   percent-1: ^<%%^>%1^</%%^>
@echo  w/o quotes: ^<~^>%~1^</~^>
@echo    fullname: ^<f^>%~f1^</f^>
@echo        disk: ^<d^>%~d1^</d^>
@echo        path: ^<p^>%~p1^</p^>
@echo        name: ^<n^>%~n1^</n^>
@echo         eXt: ^<x^>%~x1^</x^>
@echo       short: ^<s^>%~s1^</s^>
@echo        attr: ^<a^>%~a1^</a^>
@echo        time: ^<t^>%~t1^</t^>
@echo        siZe: ^<z^>%~z1^</z^>
@goto finish_label

:no_arg1_label
@echo  - arg1 is empty.

:finish_label
pause
