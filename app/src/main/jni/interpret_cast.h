#ifndef _INTERPRET_CAST_H_
#define _INTERPRET_CAST_H_

#include <iostream>
#include <string>
#include <sstream>

template<typename result_type, typename arg_type>
result_type interpret_cast(const arg_type &arg)
{
    std::stringstream interpreter;
    interpreter << arg;
    result_type result = result_type();
    interpreter >> result;
    return result;
}

#endif // _INTERPRET_CAST_H_

