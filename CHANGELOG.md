# EssentialsCore Changelog

## Version 1.0.12 (Current)

### Enhancements
- **Command Deactivation System**: Added ability to completely deactivate ApiCore commands
  - Deactivated commands will no longer function after server restart
  - Commands can be managed via `/commanddeactivate` command
  - Zero performance impact from deactivated commands
- **Enhanced Module API**: Extended the ModuleAPI interface with additional development capabilities
  - Added comprehensive resource management functions
  - Added inventory management utilities
  - Improved database operation handling
  - Added performance monitoring tools
- **Configuration Restructuring**: Complete overhaul of the config.yml file
  - Better organization with clear section headers
  - Improved documentation in comments
  - Logical grouping of related settings
- **Module Adapter System**: New module adapter for legacy and modern module support
  - Better backward compatibility
  - Simplified module development

### Bug Fixes
- Fixed ClassLoader issues with module loading
- Improved error handling in command registration
- Fixed thread safety issues in module performance tracking

### Under the Hood
- Code cleanup and optimization
- Improved JavaDoc documentation
- Enhanced error reporting 